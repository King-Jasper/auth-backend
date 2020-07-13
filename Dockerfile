FROM python:3.7.7-buster

RUN apt-get update

RUN apt-get install python3-pip -y

RUN pip3 install awscli

RUN apt-get install curl -y

RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/`curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt`/bin/linux/amd64/kubectl

RUN chmod +x ./kubectl

RUN mv ./kubectl /usr/local/bin/kubectl

ENV AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}

ENV AWS_SECRET_ACCESS_KEY=${AWS_ACCESS_KEY_ID}

ENV AWS_DEFAULT_REGION=eu-west-1