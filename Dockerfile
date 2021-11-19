FROM registry.access.redhat.com/ubi8/ubi-minimal
COPY ./target/*-runner application
EXPOSE 8080 8443
CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
