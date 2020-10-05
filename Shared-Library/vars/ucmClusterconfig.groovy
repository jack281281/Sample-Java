 pipeline {
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
  - name: busybox
    image: busybox
    command:
    - cat
    tty: true        
"""
   } 
              }        
        environment {           
            deploymentBranch = 'master'
        }
        stages {                        
            stage('demo') {
                steps {
                 println "Sample Java app demo"
		 java Hello.java
                }
            }
        }
}