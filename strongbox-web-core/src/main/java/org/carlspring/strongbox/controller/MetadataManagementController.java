package org.carlspring.strongbox.controller;

import org.carlspring.maven.commons.util.ArtifactUtils;
import org.carlspring.strongbox.security.exceptions.AuthenticationException;
import org.carlspring.strongbox.services.ArtifactMetadataService;
import org.carlspring.strongbox.storage.ArtifactStorageException;
import org.carlspring.strongbox.storage.metadata.MetadataType;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Martin Todorov
 */
@Controller
@RequestMapping("/metadata")
@Api(value = "/metadata")
@PreAuthorize("hasAuthority('ROOT')")
public class MetadataManagementController
        extends BaseArtifactController
{

    public final static String ROOT_CONTEXT = "/metadata";

    @Autowired
    private ArtifactMetadataService artifactMetadataService;

    @ApiOperation(value = "Used to rebuild the metadata for a given path.",
                  position = 0)
    @ApiResponses(value = { @ApiResponse(code = 200,
                                         message = "The metadata was successfully rebuilt!"),
                            @ApiResponse(code = 500,
                                         message = "An error occurred.") })
    @PreAuthorize("hasAuthority('MANAGEMENT_REBUILD_METADATA')")
    @RequestMapping(value = "{storageId}/{repositoryId}/{path:.+}",
                    method = RequestMethod.POST,
                    produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity rebuild(@ApiParam(value = "The storageId",
                                            required = true)
                                  @PathVariable String storageId,
                                  @ApiParam(value = "The repositoryId",
                                            required = true)
                                  @PathVariable String repositoryId,
                                  @PathVariable String path,
                                  HttpServletRequest request)
            throws IOException,
                   AuthenticationException,
                   NoSuchAlgorithmException,
                   XmlPullParserException
    {
        try
        {
            artifactMetadataService.rebuildMetadata(storageId, repositoryId, path);

            return ResponseEntity.ok("The metadata was successfully rebuilt!");
        }
        catch (ArtifactStorageException e)
        {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(e.getMessage());
        }
    }

    @ApiOperation(value = "Used to delete metadata entries for an artifact",
                  position = 0)
    @ApiResponses(value = { @ApiResponse(code = 200,
                                         message = "Successfully removed metadata entry."),
                            @ApiResponse(code = 500,
                                         message = "An error occurred.") })
    @PreAuthorize("hasAuthority('MANAGEMENT_DELETE_METADATA')")
    @RequestMapping(value = "{storageId}/{repositoryId}/{path:.+}",
                    method = RequestMethod.DELETE,
                    produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity delete(@ApiParam(value = "The storageId",
                                           required = true)
                                 @PathVariable String storageId,
                                 @ApiParam(value = "The repositoryId",
                                           required = true)
                                 @PathVariable String repositoryId,
                                 @ApiParam(value = "The version of the artifact.",
                                           required = true)
                                 @RequestParam(name = "version") String version,
                                 @ApiParam(value = "The classifier of the artifact.")
                                 @RequestParam(name = "classifier") String classifier,
                                 @ApiParam(value = "The type of metadata (artifact/snapshot/plugin).")
                                 @RequestParam(name = "metadataType") String metadataType,
                                 @PathVariable String path,
                                 HttpServletRequest request)
            throws IOException,
                   AuthenticationException,
                   NoSuchAlgorithmException,
                   XmlPullParserException
    {
        logger.debug("[delete] storageId " + storageId + " repositoryId " + repositoryId + " version " + version);

        try
        {
            if (ArtifactUtils.isReleaseVersion(version))
            {
                artifactMetadataService.removeVersion(storageId,
                                                      repositoryId,
                                                      path,
                                                      version,
                                                      MetadataType.from(metadataType));
            }
            else
            {
                artifactMetadataService.removeTimestampedSnapshotVersion(storageId,
                                                                         repositoryId,
                                                                         path,
                                                                         version,
                                                                         classifier);
            }

            return ResponseEntity.ok("Successfully removed metadata entry.");
        }
        catch (ArtifactStorageException e)
        {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(e.getMessage());
        }
    }

}
