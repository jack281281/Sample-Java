#!/usr/bin/groovy
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def params = [:]
	def dockerImage = '' 

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

	pipeline {
		agent any
		environment {
			registry = "http://ec2-18-191-210-205.us-east-2.compute.amazonaws.com/"
			registryCredential = 'nexus-id'
			deploymentBranch = 'develop'
		}

		parameters {
			string(name: 'dockerRegistry', defaultValue: 'https://356766151809.dkr.ecr.us-east-1.amazonaws.com')
        	string(name: 'az_cluster', defaultValue: 'mgmt-dev-eks-cluster')
			string(name: 'dz_cluster', defaultValue: 'mgmt-dev-eks-cluster')
			string(name: 'dz_tools_cluster', defaultValue: 'mgmt-dev-eks-cluster')
        	string(name: 'iamRole', defaultValue: 'arn:aws:iam::356766151809:role/Operations_Admin')
			string(name: 'region', defaultValue: 'us-east-1')
		}

		/*
		parameters {
			choice(name: 'projectType', choices: ['angular-library', 'angular', 'java-library', 'java-service'], description: 'Type of the project')
			string(name: 'deploymentName', description: 'Name of the deployment')
			string(name: 'dockerImageName', description: 'Docker Image Name')
			booleanParam(name: 'canSkipSonarQubeCheck', defaultValue: false, description: 'Can skip sonarqube scan?')
			booleanParam(name: 'canBuildDockerImage', defaultValue: false, description: 'Can build docker image?')
			booleanParam(name: 'canDeployImage', defaultValue: false, description: 'Can deploy docker image?')
		}
		*/

		options {
			// Only keep the 10 most recent builds
			buildDiscarder(logRotator(numToKeepStr:'10'))
		}

		stages {
			stage('Checkout') {
				steps {
					script {
						def scmVars = checkout scm
						env.IMAGE_TAG = (scmVars.GIT_COMMIT[0..6] + '.' + env.BUILD_NUMBER)

						echo "param iam role value :  ${iamRole}"
					}
				}
			}
			
			stage("Classify & Prepare") {
				steps {
					script {
						println("Job name: ${JOB_NAME} and branch is: ${BRANCH_NAME}.  Project type: ${params.projectType}")

						if (params.projectType == 'angular-library') {
							sendNotifications "Angular Library Build Started"
							copyDockerTemplate 'angular-library-dockerfile', params.dockerImageName
						} else if (params.projectType == 'angular') {
							sendNotifications "Angular Build Started"
							copyDockerTemplate 'angular-dockerfile-dev', params.dockerImageName
						} else if (params.projectType == 'java-library') {
							sendNotifications "Java Library Build Started"
						} else if (params.projectType == 'java-service') {
							sendNotifications "Java Service Build Started"
							copyDockerTemplate 'java-dockerfile', params.dockerImageName
						}
					}
				}
			}
	
			stage("Java Build") {
				when {
					expression {
						params.projectType == 'java-library' || params.projectType == 'java-service'
					}
				}

				steps {
					script {					
						withMaven(maven: 'Maven3') {
							//withSonarQubeEnv('sonarqube-server') {
								if (params.projectType == 'java-library') {
									println("Building Java Library ....")
									// org.jacoco:jacoco-maven-plugin:prepare-agent  sonar:sonar -Dsonar.branch.name=${BRANCH_NAME}
									sh 'mvn -U clean install deploy'
								} else {
									println("Building Java Service ....")
									// org.jacoco:jacoco-maven-plugin:prepare-agent sonar:sonar -Dsonar.branch.name=${BRANCH_NAME}
									sh 'mvn -U clean install'
								}
							//}
						}
					}
				}
			}
			/*
			stage("Quality Gate") {
				when {
					expression {
						(env.BRANCH_NAME == env.deploymentBranch && params.canSkipSonarQubeCheck == false) && 
						(params.projectType == 'java-library' || params.projectType == 'java-service')
					}
				}

				steps {
					timeout(time: 10, unit: 'MINUTES') {
						withSonarQubeEnv('sonarqube-server') {
							script {
								def qg = waitForQualityGate()

								if (qg.status != 'OK') {
									error "Pipeline aborted due to quality gate failure: ${qg.status}"
								}
							}
						}
					}
				}
			}
			*/

			stage("Docker Build") {
				when {
					expression {
						(env.BRANCH_NAME == env.deploymentBranch && params.canBuildDockerImage == true) && 
						(params.projectType == 'angular' ||  params.projectType == 'angular-library' ||  params.projectType == 'java-service')
					}
				}

				steps {
					sendNotifications "Creating docker image for ${JOB_NAME}"
					
					script {		
									
						withAWS(credentials: 'devops', region: "${region}", role:"${iamRole}") {							
													
							docker.withTool('docker') {			
									sh("eval \$(aws ecr get-login --no-include-email | sed 's|https://||')")							
									docker.withRegistry("${dockerRegistry}", '') {
										dockerImage = docker.build("${params.dockerImageName}:${IMAGE_TAG}")
									}
								}
					}
					}
				}
			}

			stage("Docker Push") {
				when {
					expression {
						(env.BRANCH_NAME == env.deploymentBranch && params.canBuildDockerImage == true && params.canDeployImage == true) && 
						(params.projectType == 'angular' || params.projectType == 'java-service')
					}
				}

				steps {
					sendNotifications "Staging docker image for ${JOB_NAME} to registry"
					script {
						withAWS(credentials: 'devops', region: "${region}", role:"${iamRole}") {							
						
							docker.withTool('docker') {
								docker.withRegistry("${dockerRegistry}", '') {
									dockerImage.push()
								}
							}
						}
					}
				}
			}
			
			stage ('Kubernetes Deploy') {
				when {
					expression {
						(env.BRANCH_NAME == env.deploymentBranch && params.canBuildDockerImage == true && params.canDeployImage == true) && 
						(params.projectType == 'angular' || params.projectType == 'java-service')
					}
				}

				steps {
					sendNotifications "Deploying Docker Image: ${params.dockerImageName}"
					script {
						withAWS(credentials: 'devops', region: "${region}", role:"${iamRole}") {							
							
						ucm_cluster = "" 
						
						if (params.clusterType == 'AZ') {
                            ucm_cluster = "${az_cluster}"
                        } else if (params.clusterType == 'DZ') {
                            ucm_cluster = "${dz_cluster}"
                        } else if (params.clusterType == 'DZT') {
                            ucm_cluster  = "${dz_tools_cluster}" 
                        }   


							sh "aws eks --region us-east-1 update-kubeconfig --name ${ucm_cluster} --role-arn ${iamRole}"														
				    		sh "sed -i 's/:latest/:${IMAGE_TAG}/g' k8s/deployment.yaml"							
							sh "kubectl apply -f k8s/"
						

							try {
								timeout(time: 5, unit: 'MINUTES') {
									sh "kubectl rollout status deployment/${params.deploymentName}"
								}
								sendNotifications "Successfully Deployed Docker Image: ${params.dockerImageName}"
							} catch(err) {
								sh "kubectl rollout undo deployment/${params.deploymentName}"
								sendNotifications "Deployment failed for Image: ${params.dockerImageName}.  Initiated the rollback."

								throw err
							}																					
						}
					}
				}
			}
		}
		
		post {
			always {
				sendNotifications currentBuild.result
			}
		}
	}
}
