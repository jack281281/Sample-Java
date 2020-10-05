#!/usr/bin/groovy
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def params = [:]
	def dockerImage = '' 

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = params
    body()

	pipeline {		
		agent { label 'nodejs' }
		tools {
        nodejs 'nodejs'		
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
			stage("angular setup") {
				when {
					expression {
						params.projectType == 'angular'
					}
				}

				steps {
					script {
						sh 'npm uninstall -g @angular/cli'						
						sh	'npm install -g @angular/cli'	
						sh 'npm update'											
					}
				}
			
			}
			stage("angular Build") {
				when {
					expression {
						params.projectType == 'angular'
					}
				}

				steps {
					script {
						sh 'npm uninstall -g @angular/cli'						
						sh	'npm install -g @angular/cli'	
						sh 'npm update'					
						sh 'ng build --prod --base-href /ucm-mpt'							
						zip archive: true, dir: 'dist', glob: '', zipFile: 'mcc-prep-ui.zip'					
					}
				}
			}		
			stage("upload to Nexus") {
				when {
					expression {
						expression { (branch_name =~ 'master|hotfix.*|release.*|develop|initial_app')}
					}
				}

				steps {
					nexusArtifactUploader artifacts: [
					[artifactId: "mcc-prep-ui", classifier: 'build', file:"mcc-prep-ui.zip", type: "zip"]					
					], 
					credentialsId: 'Nexus-credentials', 
					groupId: 'ucm.cms', 
					//nexusUrl: 'artifacts.ucm.tista.dev',
					nexusUrl: 'ec2-3-18-64-194.us-east-2.compute.amazonaws.com' ,
					nexusVersion: 'nexus3', 
					protocol: 'http',
					repository: 'UCMSnapshot',
					//repository: 'maven-snapshots', 
					version: "${params.ucm_version_common}-SNAPSHOT"				 		
					}
				}
			}
		
	}
}
