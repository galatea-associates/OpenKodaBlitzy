/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR
A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.repository;

import com.openkoda.core.repository.common.UnsecuredFunctionalRepositoryWithLongId;
import com.openkoda.core.security.HasSecurityRules;
import com.openkoda.model.MapEntity;
import org.springframework.stereotype.Repository;

/**
 * Repository managing MapEntity instances for geospatial data with WKT POINT coordinates.
 * <p>
 * Manages MapEntity entities storing geographic locations with Well-Known Text (WKT) POINT format
 * via JTS library. Supports geospatial queries and coordinate-based searches. Used by map services
 * for location parsing, geocoding, and spatial data visualization. Integrates with JTS Geometry
 * types for PostGIS compatibility.
 * </p>
 * <p>
 * This repository extends {@link UnsecuredFunctionalRepositoryWithLongId} to provide standard CRUD
 * operations and query methods for MapEntity objects without additional privilege enforcement.
 * Geographic coordinates are stored using JTS (Java Topology Suite) geometry types compatible with
 * PostGIS spatial database extensions.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * MapEntity location = new MapEntity();
 * location.setCoordinates(wktPoint); // WKT format: "POINT(longitude latitude)"
 * mapEntityRepository.save(location);
 * }</pre>
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see MapEntity
 * @see UnsecuredFunctionalRepositoryWithLongId
 * @see <a href="https://locationtech.github.io/jts/">JTS Topology Suite</a>
 * @see <a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry">WKT Specification</a>
 * @see <a href="https://postgis.net/">PostGIS Spatial Database</a>
 */
@Repository
public interface MapEntityRepository extends UnsecuredFunctionalRepositoryWithLongId<MapEntity>, HasSecurityRules {


}
