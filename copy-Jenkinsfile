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
"""
    }
  }
  stages {
    stage('build') {
      steps {
        container('build') {
          sh 'javac Hello.java'
          sh 'java Hello'
        }
      }
    }
    stage('test') {
      steps {
        container('build') {
          sh 'javac Test.java'
          sh 'java Test'
        }
      }
    }
    stage('deploy') {
      steps {
        container('deploy') {
          sh 'ansible-playbook -i hosts deploy.yml'
        }
      }
    }
  }
}
