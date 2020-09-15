// This is effectively a no-op Jenkinsfile for the master branch. For fuller
// examples, see other branches in this repo.
pipeline {
  agent {
    kubernetes {
      label 'hello-world'
      yaml """
apiVersion: v1
kind: Pod
spec:
  restartPolicy: Never
  containers:
  - name: busybox
    image: busybox
    command: ['cat']
    tty: true
"""
    }
  }
  stages {
    stage('say hello') {
      steps {
        container('busybox') {
          sh 'echo "hello, world"'
        }
      }
    }
  }
}
