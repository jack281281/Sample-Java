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
  - name: build
    image: openjdk:7
    command: ['cat']
    tty: true
  - name: deploy
    image: ansible/ansible:ubuntu1404
    command: ['cat']
    tty: true
  - name: copy
    image: amazon/awscli
    command: ['aws']
    tty: true
"""
    }
  }
  stages {
    stage('build') {
      steps {
        container('build') {
          sh 'javac Hello.java'
          sh 'java Hello' >> Output.txt 
		}
	    }
	}
     stage('S3-copy')) {
	steps {
	  container('copy') {
	export AWS_ACCESS_KEY_ID=ASIAVFMNLUW7AMMRKEAF
	export AWS_SECRET_ACCESS_KEY=WtbmlWhAr83u2w3x0JF22Jkbb+grs7H4OJGYoX3E
	export AWS_SESSION_TOKEN=FwoGZXIvYXdzEND//////////wEaDIFOyqwFTBsB/J1XISKoAUsgomrMsYKK5SsEZ76ixu4GK+kCn5vdUy5O4+2M/gLUPiOk/cdkSFmfJhgp57isTvHgQbSUQnaAbdwpjL5O9QRpXR+05MqIc5mk5hwJ5e9m1NnGkFIlysXrby9e1megqwtplROesKxNuYN6YadH8/21drQ4IBmUswFvBevRR+KaGg2+9XBUWvx41v+bQ/YMs3MZ+0PSLcTmCWEkqr44LQct98EBC1D2mCjx2eD8BTIt/VwX9TiaSnfQl2Fp+lMzROzWKeGHEBo9x91rsIqfFbF9L/juYybeXaFpPXQk
          	aws s3 cp Output.txt s3://haproxy-test-bucket/Output.txt
	    }
        }
      }
    }
  }
