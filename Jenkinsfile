pipeline {
    agent none
    options {
        skipStagesAfterUnstable()
    }
    environment{
     AWS_ACCESS_KEY_ID     = credentials('AWS_KEY')
     AWS_SECRET_ACCESS_KEY = credentials('AWS_Secret')
     namespace = 'staging'
     
     HOME = '.'
    }


    stages {
      
      stage('Push to kubernetes cluster'){
          agent {
          dockerfile {
              additionalBuildArgs  "--build-arg AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} --build-arg AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}"
          }
      }

      steps {
              checkout scm

              script{
                  try {
                    sh "aws eks update-kubeconfig --name mint-k8-cluster"

                    // test kubernetes config
                    sh "kubectl apply --validate=true --dry-run=client -f kubernetes/"

                    // push to kubernetes 
                    sh "kubectl apply --namespace=${namespace} -f kubernetes/"
                  }
                  catch(error){
                      throw error
                  }
              }
          }
      }

 }
}