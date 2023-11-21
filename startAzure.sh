docker run -d -p 10000:10000 -p 10001:10001 -p 10002:10002 --env AZURITE_ACCOUNTS="company1assets:key1" mcr.microsoft.com/azure-storage/azurite 
conn_str="DefaultEndpointsProtocol=http;AccountName=company1assets;AccountKey=key1;BlobEndpoint=http://127.0.0.1:10000/company1assets;"
az storage container create --name src-container --connection-string $conn_str
az storage blob upload -f ./README.md --container-name src-container --name README.md --connection-string $conn_str

docker run -d -p 20000:20000 -p 20001:20001 -p 20002:20002 --env AZURITE_ACCOUNTS="company2assets:key2" mcr.microsoft.com/azure-storage/azurite
conn_str="DefaultEndpointsProtocol=http;AccountName=company2assets;AccountKey=key2;BlobEndpoint=http://127.0.0.1:20000/company2assets;"
