/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.compat;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.Version;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.xcontent.MediaTypeParser.ParsedMediaType;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.RestCompatibilityPlugin;
import org.elasticsearch.rest.RestStatus;

import static org.elasticsearch.common.xcontent.XContentType.COMPATIBLE_WITH_PARAMETER_NAME;

public class CompatibleVersionPlugin extends Plugin implements RestCompatibilityPlugin {

    @Override
    public Version getCompatibleVersion(@Nullable ParsedMediaType acceptMediaType, @Nullable ParsedMediaType contentTypeMediaType,
                                        boolean hasContent) {
        final VersionInfo acceptVersionInfo = majorVersionFromMediaType(acceptMediaType);
        final VersionInfo contentTypeVersionInfo = majorVersionFromMediaType(contentTypeMediaType);

        // accept version must be current or prior
        if (acceptVersionInfo.version() > Version.CURRENT.major || acceptVersionInfo.version() < Version.CURRENT.major - 1) {
            throw new ElasticsearchStatusException(
                "Compatible version must be equal or less then the current version. Accept={}} Content-Type={}}",
                RestStatus.BAD_REQUEST,
                acceptMediaType, // TODO implement toString!!!
                contentTypeMediaType
            );
        }
        if (hasContent) {

            // content-type version must be current or prior
            if (contentTypeVersionInfo.version() > Version.CURRENT.major || contentTypeVersionInfo.version() < Version.CURRENT.major - 1) {
                throw new ElasticsearchStatusException(
                    "Compatible version must be equal or less then the current version. Accept={} Content-Type={}",
                    RestStatus.BAD_REQUEST,
                    acceptMediaType,
                    contentTypeMediaType,
                    RestStatus.BAD_REQUEST
                );
            }
            // if both accept and content-type are sent, the version must match
            if (contentTypeVersionInfo.version() != acceptVersionInfo.version()) {
                throw new ElasticsearchStatusException(
                    "Content-Type and Accept version requests have to match. Accept={} Content-Type={}",
                    RestStatus.BAD_REQUEST,
                    acceptMediaType,
                    contentTypeMediaType
                );
            }
            // both headers should be versioned or none
            if (contentTypeVersionInfo.isVersioned() != acceptVersionInfo.isVersioned()) {
                throw new ElasticsearchStatusException(
                    "Versioning is required on both Content-Type and Accept headers. Accept={} Content-Type={}",
                    RestStatus.BAD_REQUEST,
                    acceptMediaType,
                    contentTypeMediaType
                );
            }
            if (contentTypeVersionInfo.version() < Version.CURRENT.major) {
                return Version.CURRENT.previousMajor();
            }
        }

        if (acceptVersionInfo.version() < Version.CURRENT.major) {
            return Version.CURRENT.previousMajor();
        }

        return Version.CURRENT;
    }

    private static VersionInfo majorVersionFromMediaType(@Nullable ParsedMediaType parsedMediaType) {
        if (parsedMediaType != null) {
            String versionString = parsedMediaType.getParameters().get(COMPATIBLE_WITH_PARAMETER_NAME);
            if (versionString != null && versionString.isBlank() == false) {
                return new VersionInfo(true, Byte.parseByte(versionString));
            }
        }
        return new VersionInfo(false, Version.CURRENT.major);
    }

    private static class VersionInfo {
        private final boolean isVersioned;
        private final byte version;

        VersionInfo(boolean isVersioned, byte version) {
            this.isVersioned = isVersioned;
            this.version = version;
        }

        boolean isVersioned() {
            return isVersioned;
        }

        byte version() {
            return version;
        }
    }
}
