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

package com.openkoda.service.map;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.springframework.stereotype.Service;

/**
 * Static utility service for parsing WKT POINT geometries using JTS topology library.
 * <p>
 * This service provides geospatial coordinate parsing from Well-Known Text (WKT) format,
 * converting WKT POINT strings to JTS Point objects with validation of longitude and
 * latitude ranges. The service uses a shared static WKTReader instance for performance.
 * </p>
 * <p>
 * WKT format specification: POINT(longitude latitude) where longitude is the X coordinate
 * (-180 to 180) and latitude is the Y coordinate (-90 to 90). Example: POINT(21.0122 52.2297)
 * represents coordinates in Warsaw, Poland.
 * </p>
 * <p>
 * Validation rules enforce standard geographic coordinate ranges:
 * <ul>
 *   <li>Longitude: -180.0 to 180.0 degrees</li>
 *   <li>Latitude: -90.0 to 90.0 degrees</li>
 * </ul>
 * </p>
 * <p>
 * Thread-safety: Uses static WKTReader instance. WKTReader is thread-safe for read operations.
 * Concurrent calls to parsePoint() are safe.
 * </p>
 * <p>
 * Example usage:
 * <pre>{@code
 * Point location = MapService.parsePoint("POINT(21.0122 52.2297)");
 * double longitude = location.getX(); // 21.0122
 * }</pre>
 * </p>
 * <p>
 * Error handling: Throws RuntimeException for invalid WKT syntax, IllegalArgumentException
 * for out-of-range coordinates, and ClassCastException for non-POINT geometries.
 * </p>
 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see com.vividsolutions.jts.geom.Point
 * @see com.vividsolutions.jts.io.WKTReader
 * @see <a href="https://en.wikipedia.org/wiki/Well-known_text_representation_of_geometry">WKT Specification</a>
 */
@Service
public class MapService {
    
    /**
     * Canonical origin point at coordinates (0, 0) in WKT format.
     * <p>
     * Represents the intersection of the Prime Meridian and the Equator in the Gulf of Guinea.
     * This default point is used as a fallback value when coordinate data is missing or invalid.
     * </p>
     */
    public static final String DEFAULT_POINT = "POINT (0 0)";

    /**
     * Shared static WKT reader instance for parsing Well-Known Text geometry strings.
     * <p>
     * Using a static instance improves performance by avoiding repeated WKTReader instantiation.
     * The WKTReader class is thread-safe for read operations, making this shared instance safe
     * for concurrent access across multiple threads.
     * </p>
     */
    private static final WKTReader wtkReader = new WKTReader();

    /**
     * Parses a Well-Known Text (WKT) POINT string and validates coordinate ranges.
     * <p>
     * Converts a WKT geometry string in the format "POINT(longitude latitude)" to a JTS Point
     * object. The method validates that longitude values fall within -180 to 180 degrees and
     * latitude values fall within -90 to 90 degrees. If validation fails, an exception is thrown.
     * </p>
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Longitude (X coordinate): -180.0 ≤ longitude ≤ 180.0</li>
     *   <li>Latitude (Y coordinate): -90.0 ≤ latitude ≤ 90.0</li>
     * </ul>
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * Point location = MapService.parsePoint("POINT(21.0122 52.2297)");
     * }</pre>
     * </p>
     *
     * @param wktString the WKT POINT string to parse, in format "POINT(longitude latitude)"
     *                  where longitude is between -180 and 180, and latitude is between -90 and 90.
     *                  Example: "POINT(21.0122 52.2297)" for Warsaw coordinates
     * @return a JTS Point object with validated longitude and latitude coordinates
     * @throws RuntimeException if the WKT string has invalid syntax and cannot be parsed.
     *                          This wraps the underlying ParseException from the JTS library
     * @throws IllegalArgumentException if longitude is outside the range [-180, 180] or
     *                                  latitude is outside the range [-90, 90]
     * @throws ClassCastException if the parsed WKT geometry is not a POINT type
     *                            (e.g., LINESTRING or POLYGON)
     */
    public static Point parsePoint(String s) {
        try {
            Point point = (Point) wtkReader.read(s);
            if(!(point.getX() <= 180.0) || !(point.getX() >= -180.0)){
                throw new IllegalArgumentException("Longitude should between -180 and 180");
            }
            else if (!(point.getY() <= 90.0) || !(point.getY() >= -90.0)){
                throw new IllegalArgumentException("Latitude should between -180 and 180");
            }
            return point;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
