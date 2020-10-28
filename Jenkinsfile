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
    image: openjdk:8
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
			sh 'curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py'
			sh 'python get-pip.py'
			sh 'pip install awscli'
			sh 'export AWS_ACCESS_KEY_ID=ASIAVFMNLUW7DPOICGGP'
			sh 'export AWS_SECRET_ACCESS_KEY=x4YkbQ1P55A/5dP1/KRZVL5gvgiEuOoPAyg4ac0m'
			sh 'export AWS_SESSION_TOKEN=FwoGZXIvYXdzEOj//////////wEaDK//XvEceT9tXIbycyKoAe9uc4WmtHTP9DhGAoOPlGuxxk7BZtEskPuM6K7V2qOgXYsu2X/6Ij7rB9hoitutpTvaJOI+m1OQ956p8/br1dXHe5d1JEFYDlTheQCNL4TeaPrg+8I1vAhyN9GB2pNVP1glYLRfDHKmpo3gYYIwJ3expvH/9FIVfLXxQ03fvZYQbV6uuopEHPLxC+GrGtiliygUozhmPU35deuTo/K+oT5bZ4Rk8ov8bijk+uX8BTIt0FgRSHvLGW56J0jBms7SiE6uFr6wrzYE70Bd37To4qjfnHrYpvV9zqL3S5I3'
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