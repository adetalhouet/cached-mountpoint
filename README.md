# cached-mountpoint
A dummy yang model was placed in the repo for demo purposes.

Note: for those knowing the NETCONF mount point process in ODL, this is equivalent to sideloading models. 
One of the enhancement would be to create an API to put models into the appropriate folder, and then create the mount point that will dynamically load those models.

Here is a small postman collection to play with this example: https://www.getpostman.com/collections/e8c28a7d1e3354e5f71e

## Setup the cache repository with the models to dynamically load
Create a folder under `cache/cached-mountpoint` and put there the yang models you'll dynamically mount when required.
The files has to follow this convention: moduleName@revisionDate.yang

## Create the cached mount point

* The mount point will be called `testCachedMountPoint`.
* Specify the folder name you created in the previous step, here `testCachedMountPoint`.
* Add the capabilities under `yang-module-capabilities`, as shown in the example bellow. Note, you'll have to make sure that the capabilities you provide in the cache/cached-mountpoint/XXX directory are able to resolved their imports.

```
curl -X PUT -H "Authorization: Basic YWRtaW46YWRtaW4=" -H "Accept: application/json" -H "Content-Type: application/xml" -H  -d 
'<node xmlns="urn:TBD:params:xml:ns:yang:network-topology">
	<node-id>testCachedMountPoint</node-id>
	<schema-cache-directory xmlns="urn:opendaylight:params:xml:ns:yang:cached-mountpoint">testCachedMountPoint</schema-cache-directory>
	<yang-module-capabilities xmlns="urn:opendaylight:params:xml:ns:yang:cached-mountpoint">
		<capability>urn:opendaylight:car?module=car&amp;revision=2016-06-09</capability>
   </yang-module-capabilities>
</node>' "http://localhost:8181/restconf/config/network-topology:network-topology/topology/cached-mountpoint/node/testCachedMountPoint"
```

## Interact with your mountpoint

Once your mount point is created, you can read/write to the loaded models, through `yang-ext:mount`.

### Write
```
curl -X PUT -H "Authorization: Basic YWRtaW46YWRtaW4=" -H "Content-Type: application/xml" -d 
'<car-info xmlns="urn:opendaylight:car">
	<car-id>2</car-id>
	<max-speed>100</max-speed>
</car-info>
' "http://localhost:8181/restconf/config/network-topology:network-topology/topology/cached-mountpoint/node/testCachedMountPoint/yang-ext:mount/car:car-info"
```

### Read
```
curl -X GET -H "http://localhost:8181/restconf/config/network-topology:network-topology/topology/cached-mountpoint/node/testCachedMountPoint/yang-ext:mount/car:car-info"
```