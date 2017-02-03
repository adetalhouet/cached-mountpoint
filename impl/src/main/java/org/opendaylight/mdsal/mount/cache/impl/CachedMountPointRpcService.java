package org.opendaylight.mdsal.mount.cache.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.cached.mount.point.rev170201.CachedMountPointService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.cached.mount.point.rev170201.LoadModelsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.cached.mount.point.rev170201.LoadModelsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.cached.mount.point.rev170201.LoadModelsOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by adetalhouet on 2017-02-03.
 */
public class CachedMountPointRpcService implements CachedMountPointService {

    private static final Logger LOG = LoggerFactory.getLogger(CachedMountPointRpcService.class);

    @Override
    public Future<RpcResult<LoadModelsOutput>> loadModels(LoadModelsInput input) {
        final Path outputFolder = Paths.get(CachedSchemaRepository.DEFAULT_CACHED_MOUNT_POINT_DIRECTORY, input.getSchemaCacheDirectory());
        if (Files.exists(outputFolder)) {
            LOG.debug("Folder already exist {}", outputFolder.toString());
            return handleResponse(LoadModelsOutput.Status.ALREADYEXIST);
        } else {
            try {
                Files.createDirectories(outputFolder);
            } catch (IOException e) {
                LOG.error("Failed to create directory={}", outputFolder, e);
                return handleResponse(e.getMessage());
            }
        }
        final Path models = Paths.get(input.getPath());
        if (Files.isDirectory(models)) {
            try {
                Stream<Path> files = Files.list(models);
                files.forEach(f -> {
                    try {
                        Files.copy(f, outputFolder.resolve(f.getFileName()));
                    } catch (IOException e) {
                        LOG.error("Failed to copy file={} to folder={}", f, outputFolder, e);
                    }
                });
            } catch (IOException e) {
                LOG.error("Failed to list the files in directory={}", models, e);
                return handleResponse(e.getMessage());
            }

        } else {
            try {
                Files.copy(models, outputFolder.resolve(models.getFileName()));
            } catch (IOException e) {
                LOG.error("Failed to copy file={} to folder={}", models, outputFolder, e);
                return handleResponse(e.getMessage());
            }
        }
        return handleResponse(LoadModelsOutput.Status.CREATED);
    }

    private Future<RpcResult<LoadModelsOutput>> handleResponse(final LoadModelsOutput.Status status) {
        final LoadModelsOutput res = new LoadModelsOutputBuilder()
                .setStatus(status)
                .build();
        return RpcResultBuilder.success(res).buildFuture();
    }

    private Future<RpcResult<LoadModelsOutput>> handleResponse(final String message) {
        final LoadModelsOutput res = new LoadModelsOutputBuilder()
                .setStatus(LoadModelsOutput.Status.ERROR)
                .setErrorMessage(message)
                .build();
        return RpcResultBuilder.success(res).buildFuture();
    }
}
