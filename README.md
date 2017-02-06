# cached-mountpoint
Purpose is to dynamically load yang models, and to be able to interact with them.

A dummy yang model was placed in the repo for demo purposes.

Note: for those knowing the NETCONF mount point process in ODL, this is equivalent to sideloading models. 

## Setup the cache repository with the models to dynamically load
The files has to follow this convention: moduleName@revisionDate.yang

### Manually
Create a folder under `{KARAF_HOME}/cache/cached-mountpoint` and put there the yang models you'll dynamically mount when required.
For our example, will we create the folder name `directory-1` and put the car.yang in there.

### Using an RPC
An RPC was defined to deploy folder at runtime providing the yang models.
Set the `path` to a folder or to a yang file, and the destination folder to be created.
If the folder already exist, you can overwrite it by specifying `overwrite=true`.

```
curl -X POST -H "Authorization: Basic YWRtaW46YWRtaW4=" -H "Accept: application/json" -H "Content-Type: application/xml" -d 
'<input xmlns="urn:opendaylight:params:xml:ns:yang:cached-mount-point">
    <path>/Users/adetalhouet/Desktop/test</path>
    <schema-cache-directory>directory-1</schema-cache-directory>
    <overwrite>false</overwrite>
</input>' "http://localhost:8181/restconf/operations/cached-mount-point:load-models"
```

## Create the cached mount point

* The mount point will be called `testCachedMountPoint`.
* Specify the folder name you created in the previous step, here `cached-mountpoint-1`.
* Add the capabilities under `yang-module-capabilities`, as shown in the example bellow. 

Note, you'll have to make sure that the capabilities you provide in the cache/cached-mountpoint/... directory are able to resolved their imports.

```
curl -X PUT -H "Authorization: Basic YWRtaW46YWRtaW4=" -H "Accept: application/json" -H "Content-Type: application/xml" -H  -d 
'<node xmlns="urn:TBD:params:xml:ns:yang:network-topology">
	<node-id>cachedMountPoint1</node-id>
	<schema-cache-directory xmlns="urn:opendaylight:params:xml:ns:yang:cached-mountpoint">cached-mountpoint-1</schema-cache-directory>
	<yang-module-capabilities xmlns="urn:opendaylight:params:xml:ns:yang:cached-mountpoint">
		<capability>urn:opendaylight:car?module=car&amp;revision=2016-06-09</capability>
   </yang-module-capabilities>
</node>' "http://localhost:8181/restconf/config/network-topology:network-topology/topology/cached-mountpoint/node/cachedMountPoint1"
```

On creation of the cached mount point, a scan of all the yang files provided in previous step will be perform, and a generic DataTreeChangeListener will be registered for all the top level `container`.

## Interact with your mount point

Once your mount point is created, you can read/write to the loaded models, through `yang-ext:mount`.

### Write
```
curl -X PUT -H "Authorization: Basic YWRtaW46YWRtaW4=" -H "Content-Type: application/xml" -d 
'<car-info xmlns="urn:opendaylight:car">
	<car-id>2</car-id>
	<max-speed>100</max-speed>
</car-info>
' "http://localhost:8181/restconf/config/network-topology:network-topology/topology/cached-mountpoint/node/cachedMountPoint1/yang-ext:mount/car:car-info"
```

### Read
```
curl -X GET -H "http://localhost:8181/restconf/config/network-topology:network-topology/topology/cached-mountpoint/node/cachedMountPoint1/yang-ext:mount/car:car-info"
```

### Cached mount point topology
```
curl -X GET -H "Authorization: Basic YWRtaW46YWRtaW4=" -H "Accept: application/json" -H "Content-Type: application/xml" "http://localhost:8181/restconf/config/network-topology:network-topology/topology/cached-mount-point"
```

## Resources
Here is a small postman collection to play with this example: https://www.getpostman.com/collections/e8c28a7d1e3354e5f71e

# Credits

* https://github.com/CiscoDevNet/opendaylight-bootcamps/tree/master/2015-11-Princeton/caching-mountpoint
* https://github.com/marosmars/caching-mountpoint