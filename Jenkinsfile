// This is effectively a no-op Jenkinsfile for the master branch. For fuller
// examples, see other branches in this repo.
pipeline {
  agent {
    kubernetes {
      label 'java-build'
      yaml """
apiVersion: v1
kind: Pod
spec:
  restartPolicy: Never
  containers:
  - name: openjdk
    image: openjdk:7
    command: ['cat']
    tty: true
"""
    }
  }
  stages {
    stage('build') {
      steps {
        container('openjdk') {
          	sh 'javac Hello.java'
          	sh 'java Hello >> Output.txt'
	  		sh 'pwd'
	  		sh 'ls -l'
	  		sh 'df -h'
	  		RUN apt-get install python-pip
			RUN pip install awscli
			sh 'aws s3 cp Output.txt s3://haproxy-test-bucket/Output.txt'
	  }
	 }
	}
    stage('copytos3') {
	steps {
	    container('openjdk') {
       }
     }
   }
 }
