# Sample-Java

## Objective

The goal here is to serve as a reference pipeline for our internal Cloudbees Core offering.

This pipeline builds a [Spring Boot Microservice](https://github.com/in28minutes/spring-microservices/tree/master/03.microservices/currency-conversion-service).

The pipeline does the following:

1. Stage 1: Build
    1. Build the java artifact for the service
    1. Push artifact to S3 (in the ADO Sandbox account)

1. Stage 2: Test
    1. For now skip this with a "echo hello world"

1. Stage 3: Docker Build
    1. Build a docker image with openjdk7 and the artifact from S3
    1. Push Docker image to ECR (in the ADO Sandbox account)

1. Stage 4: Deploy
    1. Deploy the container to AWS Fargate (in an existing ECS/Fargate Cluster)


### References

1. https://github.com/stacksimplify/docker-fundamentals
1. https://github.com/stacksimplify/aws-fargate-ecs-masterclass/tree/master/03-Fargate-and-ECS-Fundamentals
1. https://tomgregory.com/deploying-a-spring-boot-application-into-aws-with-jenkins/
