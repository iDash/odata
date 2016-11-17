/**
 * Copyright (c) 2014 All Rights Reserved by the SDL Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sdl.odata.renderer.json;

import static com.sdl.odata.api.processor.query.QueryResult.ResultType.COLLECTION;
import static com.sdl.odata.api.processor.query.QueryResult.ResultType.RAW_JSON;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sdl.odata.api.ODataException;
import com.sdl.odata.api.ODataSystemException;
import com.sdl.odata.api.processor.query.QueryResult;
import com.sdl.odata.api.service.MediaType;
import com.sdl.odata.api.service.ODataRequestContext;
import com.sdl.odata.api.service.ODataResponse;
import com.sdl.odata.renderer.AbstractJsonRenderer;
import com.sdl.odata.renderer.json.writer.JsonWriter;

/**
 * Renderer which renders data in OData JSON format.
 * Reference: <a href="http://docs.oasis-open.org/odata/odata-json-format/v4.0/os/odata-json-format-v4.0-os.html">
 * OData Atom Format Version 4.0 specification</a>
 *
 * Copyright (c) 2016 All rights reserved by Siemens AG
 * handling raw json result
 */
@Component
public final class JsonRenderer extends AbstractJsonRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(JsonRenderer.class);

    @Override
    public int score(final ODataRequestContext requestContext, final QueryResult data) {

        // This renderer only handles entity queries
        if (!isEntityQuery(requestContext.getUri(), requestContext.getEntityDataModel())) {
            return 0;
        }

        final int returnScore = super.score(requestContext, data);
        LOG.debug("Renderer score is {}", returnScore);

        return returnScore;
    }

    @Override
    public void render(final ODataRequestContext requestContext, final QueryResult result,
            final ODataResponse.Builder responseBuilder) throws ODataException {

        LOG.debug("Start rendering entity(es) for request: {}", requestContext);

        final JsonWriter writer = new JsonWriter(requestContext.getUri(), requestContext.getEntityDataModel());

        final String contextUrl = buildContextURL(requestContext, result.getData());
        String json;
        if (result.getType() == COLLECTION) {
            json = writer.writeFeed((List<?>) result.getData(), contextUrl, result.getMeta());
        } else if (result.getType() == RAW_JSON) {
            json = writer.writeRawJson(result.getData().toString(), contextUrl);
        } else {
            json = writer.writeEntry(result.getData(), contextUrl);
        }
        if (responseBuilder != null) {
            try {
                responseBuilder
                        .setContentType(MediaType.JSON)
                        .setHeader("OData-Version", ODATA_VERSION_HEADER)
                        .setBodyText(json, StandardCharsets.UTF_8.name());
            } catch (final UnsupportedEncodingException e) {
                throw new ODataSystemException(e);
            }
        }

        LOG.debug("End rendering entity(es) for request: {}", requestContext);
    }
}
