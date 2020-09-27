# Notes

```bash
xml sel -t -v "//postaja" hacking/hidro_podatki_zadnji.xml
```

## OPS

> helm upgrade app charts/pinkstack-rsvp -f charts/pinkstack-rsvp/values.yaml --set image.tag=0.1.3
- https://github.com/pinkstack/pinkstack-rsvp/blob/master/kubernetes/rsvp-deployment.yaml

# repozitory

# Log

kubectl create namespace one

helm install one chart/voda --atomic --values chart/voda/values.yaml

Login to Container registry

`az acr login --name vodacr --resource-group voda-cr`

Connect KE <-> Repository w/

`az aks update -n myAKSCluster -g aks-rg --attach-acr vodacr`

az aks get-credentials --resource-group aks-rg --name myAKSCluster


--attach-acr


az aks get-credentials --resource-group aks-rg --name myAKSCluster


az aks update --resource-group aks-rg --name myAKSCluster --attach-acr vodacr

eBFXiJZ4n=XBe/GrZHLuesCTN0cHSmdg

az aks update --attach-acr vodacr --name myAKSCluster --resource-group aks-rg


https://github.com/Azure/AKS/issues/1517#issuecomment-676441378


az aks get-credentials -g aks-rg -n myAKSCluster --admin

fix

This works
`az acr credential show --name vodacr --resource-group voda-cr`

vodacr

kubectl create secret docker-registry acr-secret `
  --docker-server=vodacr.azurecr.io `
  --docker-username=vodacr `
  --docker-password=eBFXiJZ4n=XBe/GrZHLuesCTN0cHSmdg
  
kubectl create secret docker-registry acr-secret \
  --docker-server=vodacr.azurecr.io \
  --docker-username=vodacr \
  --docker-password="eBFXiJZ4n=XBe/GrZHLuesCTN0cHSmdg"

  
### Deploy dlow

kubectl apply -f k8s/set-keys.yaml
sbt docker:publish
helm upgrade one chart/voda -f chart/voda/values.yaml --set image.tag=0.1.0

# Run archive task
kubectl apply -f k8s/start-archive-collection.yaml