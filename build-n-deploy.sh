export DOCKER_HOST="unix://$(podman info -f "{{.Host.RemoteSocket.Path}}")"

cd /media/sf_2019768/faas-workspace/50661-assemble-test-containers/

mvn clean package
