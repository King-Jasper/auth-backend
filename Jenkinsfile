pipeline {
    agent any
    options {
        skipStagesAfterUnstable()
    }
    environment{
        AWS_ACCESS_KEY_ID     = credentials('AWS_KEY')
        imageTag = "${env.BUILD_NUMBER}"
        AWS_SECRET_ACCESS_KEY = credentials('AWS_Secret')
        stagingNamespace = "staging"
        productionNamespace = "production"
        defaultDeploymentNamespace = "staging"
        defaultAppProfile = "sandbox"
        dockerImageName = "mintfintech/savings-service"
        HOME = '.'
    }
    stages {
        
        stage('Mvn Clean Package') {
            steps {
                sh "mvn clean package"
            }
        }
        stage('Build Docker Image') {
            steps {
                sh 'cp target/savings-service.jar docker'
                script {
                    if ("${env.BRANCH_NAME}".equalsIgnoreCase("production")) {
                        sh 'cp target/newrelic/newrelic.jar docker'
                        sh "cd docker && docker image build -f Dockerfile_prod -t ${dockerImageName}:${imageTag} ."
                    } else {
                        sh "cd docker && docker image build -t ${dockerImageName}:${imageTag} ."
                    }
                }
            }
        }
        stage('Push Docker Image') {
            // push image only when branch is production or staging
            steps {
                script {
                    withCredentials([string(credentialsId: 'mint-docker-password', variable: 'dockerhubPassword')]) {
                        sh "docker login -u mintfintech -p ${dockerhubPassword}"
                    }
                    if ("${env.BRANCH_NAME}".equalsIgnoreCase("production")) {
                        sh "docker tag ${dockerImageName}:${imageTag} ${dockerImageName}:prod-${imageTag}"
                        sh "docker push ${dockerImageName}:prod-${imageTag}"
                        // delete image to free up space
                        sh "docker rmi ${dockerImageName}:prod-${imageTag}"

                    } else if ("${env.BRANCH_NAME}".equalsIgnoreCase("staging")) {
                        sh "docker tag ${dockerImageName}:${imageTag} ${dockerImageName}:staging-${imageTag}"
                        sh "docker push ${dockerImageName}:staging-${imageTag}"
                        // delete image to free up space
                        sh "docker rmi ${dockerImageName}:staging-${imageTag}"

                    } else if ("${env.BRANCH_NAME}".equalsIgnoreCase("sandbox")) {
                        sh "docker tag ${dockerImageName}:${imageTag} ${dockerImageName}:sandbox-${imageTag}"
                        sh "docker push ${dockerImageName}:sandbox-${imageTag}"
                        // delete image to free up space
                        sh "docker rmi ${dockerImageName}:sandbox-${imageTag}"

                    } else {
                        //delete docker image
                        sh "docker rmi ${dockerImageName}:${imageTag}"
                    }
                }
            }
        }
        stage('push to production or staging') {
            agent {
                dockerfile {
                    additionalBuildArgs "--build-arg AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} --build-arg AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}"
                }
            }
            steps {
                script {
                    if ("${env.BRANCH_NAME}".equalsIgnoreCase("production")) {
                        try {
                            sh "aws eks update-kubeconfig --name mint-k8-cluster"
                            //change image tag in the deployment file
                            sh("sed -i.bak 's|${dockerImageName}|${dockerImageName}:prod-${env.BUILD_NUMBER}|' ./kubernetes/deployment.yaml")
                            sh("sed -i.bak 's|${defaultDeploymentNamespace}|production|' ./kubernetes/deployment.yaml")
                            sh("sed -i.bak 's|${defaultDeploymentNamespace}|production|' ./kubernetes/cluster_role.yaml")
                            sh("sed -i.bak 's|${defaultAppProfile}|prod|' ./kubernetes/deployment.yaml")
                            sh 'kubectl apply --validate=true --dry-run=client -f kubernetes/'
                            sh "kubectl apply --namespace=${productionNamespace}  -f kubernetes/"
                        }
                        catch (error) {
                            throw error
                        }
                    } else if ("${env.BRANCH_NAME}".equalsIgnoreCase("sandbox")) {
                        try {
                            sh "aws eks update-kubeconfig --name mint-k8-cluster"
                            //change image tag in the deployment file
                            sh("sed -i.bak 's|${dockerImageName}|${dockerImageName}:sandbox-${env.BUILD_NUMBER}|' ./kubernetes/deployment.yaml")
                            sh("sed -i.bak 's|${defaultDeploymentNamespace}|staging|' ./kubernetes/deployment.yaml")
                            sh("sed -i.bak 's|${defaultAppProfile}|sandbox|' ./kubernetes/deployment.yaml")
                            sh 'kubectl apply --validate=true --dry-run=client -f kubernetes/'
                            sh "kubectl apply --namespace=${stagingNamespace}  -f kubernetes/"
                        }
                        catch (error) {
                            throw error
                        }
                    }
                }
            }
        }
    }
}
