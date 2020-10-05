#!/usr/bin/groovy
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def params = [:]
	def dockerImage = '' 

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

	pipeline {

		 tools{			
			 maven 'Maven3'
			 nodejs 'nodejs'
			 dockerTool 'docker'
		  }
		
			  agent {
    kubernetes {	 

        yaml """
apiVersion: v1
kind: Pod
metadata:
labels:
  component: ci
spec:
  # Use service account that can deploy to all namespaces  
  containers: 
  - name: buildtools
    image: senthilvv28/buildertools:latest
    command:
    - cat
    tty: true  
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375 
  - name: docker
    image: docker:19.03.1
    command:
    - sleep
    args:
    - 99d
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
  - name: docker-daemon
    image: docker:19.03.1-dind
    securityContext:
      privileged: true
    env:
      - name: DOCKER_TLS_CERTDIR
        value: "" 
  - name: jfrogcli
    image: docker.bintray.io/jfrog/jfrog-cli-go
    command: ['cat']
    tty: true
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
    resources:
      limits:
        cpu: 1000m
        memory: 1024Mi
"""
   } 
			  }		
		environment {
			registry = "http://ec2-18-191-210-205.us-east-2.compute.amazonaws.com/"
			registryCredential = 'nexus-id'
			deploymentBranch = 'master'
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
					container('buildtools') {				
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

			// stage("Docker Build") {
			// 	when {
			// 		expression {
			// 			(env.BRANCH_NAME == env.deploymentBranch && params.canBuildDockerImage == true) && 
			// 			(params.projectType == 'angular' ||  params.projectType == 'angular-library' ||  params.projectType == 'java-service')
			// 		}
			// 	}

			// 	steps {
			// 		sendNotifications "Creating docker image for ${JOB_NAME}"
					
			// 		script {		
			// 		 container('docker') {									
			// 			withAWS(credentials: 'devops', region: "${region}", role:"${iamRole}") {							
													
			// 				docker.withTool('docker') {		
								    
														
			// 						docker.withRegistry("${dockerRegistry}", '') {
			// 							dockerImage = docker.build("${params.dockerImageName}:${IMAGE_TAG}")
			// 						}
			// 					}
			// 			}
			// 		}
			// 		}
			// 	}
			// }

			// stage("Docker Push") {
			// 	when {
			// 		expression {
			// 			(env.BRANCH_NAME == env.deploymentBranch && params.canBuildDockerImage == true && params.canDeployImage == true) && 
			// 			(params.projectType == 'angular' || params.projectType == 'java-service')
			// 		}
			// 	}

			// 	steps {
			// 		sendNotifications "Staging docker image for ${JOB_NAME} to registry"
			// 		script {
			// 			container('buildtools') {
			// 			withAWS(credentials: 'devops', region: "${region}", role:"${iamRole}") {							
			// 			 sh("eval \$(aws ecr get-login --no-include-email | sed 's|https://||')")		
			// 				docker.withTool('docker') {
			// 					docker.withRegistry("${dockerRegistry}", '') {
			// 						dockerImage.push()
			// 					}
			// 				}
			// 			}
			// 			}
			// 		}
			// 	}
			// }

	// 		stage('jfrogpush') {
    //          steps {
    //           container('jfrogcli') {
    //           withCredentials([usernamePassword(credentialsId: 'jfrogauth', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
    //            sh '''        
	// 		    apk update && apk add --no-cache docker
	// 			apk search -v -d 'docker'
    //             docker version
    //             jfrog rt config cms-artifactory --url=https://artifactory.cloud.cms.gov/artifactory --user="$USER" --password="$PASS"
    //             jfrog rt config show cms-artifactory
	// 			apk search -v -d 'docker'
    //             docker version
    //             jfrog rt docker-push artifactory.cloud.cms.gov/artifactory/ucm-ng-docker/workflow-proxy-service:latest tooling-docker-registry --build-name=workflow-proxy-service --build-number=${BUILD_NUMBER}		
    //         '''
    //       }
    //     }
    //   }
	// 		}


			stage('jfrogpush') {

             steps {
				 container('buildtools') {
              script{
				  docker.withRegistry('https://artifactory.cloud.cms.gov', 'jfrogauth'){
				   docker.build("ucm-ng-docker/${params.dockerImageName}:${IMAGE_TAG}").push("${IMAGE_TAG}")		
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
						container('buildtools') {	
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
		}
		
		post {
			always {
				sendNotifications currentBuild.result
			}
		}
	}
}
