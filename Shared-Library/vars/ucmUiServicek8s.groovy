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
		  }	
			  agent {
    kubernetes {	 

        yaml """
apiVersion: v1
kind: Pod
spec: 
  containers: 
    - 
      command: 
        - tail
        - "-f"
        - /dev/null
      image: "maven:3.5.4-jdk-8-slim"
      imagePullPolicy: Always
      name: maven
      resources: 
        limits: 
          memory: 8Gi
        requests: 
          cpu: 500m
          memory: 8Gi
    - 
      command: 
        - tail
        - "-f"
        - /dev/null
      image: "docker:18.06.1"
      imagePullPolicy: Always
      name: docker
      volumeMounts: 
        - 
          mountPath: /var/run/docker.sock
          name: docker
  volumes: 
    - 
      hostPath: 
        path: /var/run/docker.sock
      name: docker
"""
   } 
	    }		
		environment {			
			registryCredential = 'nexus-id'
			deploymentBranch = 'develop'
		}		

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
					}
				}
			}			
			
			 stage ('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                '''
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

							container('maven') {				
						withMaven(maven: 'Maven3') {
							//withSonarQubeEnv('sonarqube-server') {
								if (params.projectType == 'java-library') {
									println("Building Java Library ....")
									// org.jacoco:jacoco-maven-plugin:prepare-agent  sonar:sonar -Dsonar.branch.name=${BRANCH_NAME}
									
									//sh 'mvn -U clean install deploy'
								} else {
									println("Building Java Service ....")
									// org.jacoco:jacoco-maven-plugin:prepare-agent sonar:sonar -Dsonar.branch.name=${BRANCH_NAME}
									sh 'mvn  -U clean install deploy'
								}
							//}
						}
					}			

								 //sh "mvn dependency:get -DrepoUrl=http://172.28.1.2:8081/repository/mcc-snapshot/ -Dartifact=ucm2.cms:mcc-prep-ui:1.0.0-SNAPSHOT:zip:build -Ddest=mcc-prep-ui.zip -Dtransitive=false"

								//	unzip zipFile: 'mcc-prep-ui.zip' 
									
									//Create a new dir  /src/main/resources
								//	sh "mkdir -p  src/main/resources"
								
									//
								 //   sh "cp -aR mcc-prep-ui/*  src/main/resources"

								//	println("Building Java Service ....")																	
									//sh 'mvn -s /var/jenkins_home/settings.xml -U clean install deploy'													
					}
				}
			}
		}

			  

	


	}
}
