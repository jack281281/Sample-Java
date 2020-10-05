#!/usr/bin/groovy

def call(String dockerFileTemplateName = '', String serviceName = '') {
    // Load script from library with package path
    def dockerfile = libraryResource dockerFileTemplateName

    // create a file with dockerfile content
    writeFile file: './Dockerfile', text: dockerfile

    if (serviceName?.trim()) {
        sh "sed -i 's/SERVICE_NAME/${serviceName}/g' ./Dockerfile"
    }
}