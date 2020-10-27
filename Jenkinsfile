/// This is effectively a no-op Jenkinsfile for the master branch. For fuller
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
    image: amazoncorretto:8
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
			sh 'which aws'
			sh 'aws s3 ls'
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
}