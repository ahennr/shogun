/* SHOGun, https://terrestris.github.io/shogun/
 *
 * Copyright © 2020-present terrestris GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.terrestris.shogun.lib.graphql.scalar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.*;
import lombok.extern.log4j.Log4j2;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
public class GeometryScalar {

    private static ObjectMapper om;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        GeometryScalar.om = objectMapper;
    }

    public static GraphQLScalarType INSTANCE;

    static {
        Coercing<Geometry, Map<String, Object>> coercing = new Coercing<>() {

            @Override
            public Map<String, Object> serialize(Object dataFetcherResult) throws CoercingSerializeException {
                if (dataFetcherResult == null) {
                    throw new CoercingSerializeException("Unable to serialize geometry since input is null");
                }
                if (dataFetcherResult instanceof Geometry geometry) {
                    try {
                        return om.readValue(om.writeValueAsString(geometry), HashMap.class);
                    } catch (JsonProcessingException e) {
                        log.error("JSON Processing error while writing geometry for GraphQL");
                        log.trace("Full stack trace: ", e);
                        throw new CoercingSerializeException(e.getMessage());
                    }
                } else {
                    throw new CoercingSerializeException("Unable to serialize " + dataFetcherResult + " as a geometry");
                }
            }

            @Override
            public Geometry parseValue(Object input) throws CoercingParseValueException {
                if (input == null) {
                    throw new CoercingSerializeException("Unable to parse geometry value since input is null");
                }
                if (input instanceof Map<?, ?> geometryMap) {
                    try {
                        return om.readValue(om.writeValueAsString(geometryMap), Geometry.class);
                    } catch (JsonProcessingException e) {
                        throw new CoercingParseValueException("Unable to parse variable value " + input + " as a geometry");
                    }
                }
                throw new CoercingParseValueException("Unable to parse variable value " + input + " as a geometry");

            }

            @Override
            public Geometry parseLiteral(Object input) throws CoercingParseLiteralException {
                if (input instanceof StringValue) {
                    String geometryString = ((StringValue) input).getValue();
                    try {
                        return om.readValue(geometryString, Geometry.class);
                    } catch (JsonProcessingException e) {
                        throw new CoercingParseValueException("Unable to parse value " + input + " as a geometry");
                    }
                }

                // TODO: check this

//                if (input instanceof ObjectValue objectValue) {
//                    objectValue.getAdditionalData();
//                    try {
//                        return om.readValue(geometryString, HashMap.class);
//                    } catch (JsonProcessingException e) {
//                        throw new CoercingParseValueException("Unable to parse value " + input + " as a geometry");
//                    }
//                }
                throw new CoercingParseLiteralException(
                    "Value is not an geometry"
                );

            }

            @Override
            public Geometry parseLiteral(Object input, Map<String, Object> variables) throws CoercingParseLiteralException {
                return Coercing.super.parseLiteral(input, variables);
            }

            @Override
            public Value valueToLiteral(Object input) {
                return Coercing.super.valueToLiteral(input);
            }
        };

        INSTANCE = GraphQLScalarType
            .newScalar()
            .name("Geometry")
            .description("A custom scalar that handles geometry")
            .coercing(coercing)
            .build();
    }

}
