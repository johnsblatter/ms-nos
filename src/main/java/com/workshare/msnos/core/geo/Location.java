package com.workshare.msnos.core.geo;

import com.maxmind.geoip2.model.OmniResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Continent;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Subdivision;
import com.workshare.msnos.soup.json.Json;

public class Location {

    public static Place NOWHERE = new Place(Place.Type.CONTINENT, "Nowhere", "NN") {
        @Override
        public boolean equals(Object obj) {
            return false;
        }
    };

    public static class Place {
        public enum Type {
            CONTINENT, COUNTRY, REGION, CITY
        }

        private final Type type;
        private final String name;
        private final String code;

        public Place(Type type, String name, String code) {
            if (type == null || code == null || name == null)
                throw new IllegalArgumentException("No constructor parameter can be null!");

            this.type = type;
            this.code = code;
            this.name = name;
        }

        public Type getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }

        @Override
        public String toString() {
            return Json.toJsonString(this);
        }

        @Override
        public int hashCode() {
            final int prime = 17;
            int result = 1;
            result = prime * result + code.hashCode();
            result = prime * result + type.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            try {
                Place other = (Place) obj;
                return type == other.type && code.equals(other.code);
            } catch (Exception ignore) {
                return false;
            }
        }
    }

    public class Match {

        private final int value;
        private final Location source;
        private final Location target;

        public Match(Location source, Location target) {
            this.source = source;
            this.target = target;
            this.value = calculateValue(source, target);
        }

        private int calculateValue(Location source, Location target) {
            int total = 0;
            total += source.continent.equals(target.continent) ? 1 : 0;
            total += source.country.equals(target.country) ? 2 : 0;
            total += source.region.equals(target.region) ? 4 : 0;
            total += source.city.equals(target.city) ? 8 : 0;
            return total;
        }

        public Location getSource() {
            return source;
        }

        public Location getTarget() {
            return target;
        }

        public int value() {
            return value;
        }
    }

    private final Place continent;
    private final Place country;
    private final Place region;
    private final Place city;

    public Location(OmniResponse response) {
        this.continent = (response.getContinent() == null ? NOWHERE : makeContinent(response.getContinent()));
        this.country = (response.getCountry() == null ? NOWHERE : makeCountry(response.getCountry()));
        ;
        this.region = (response.getMostSpecificSubdivision() == null ? NOWHERE : makeRegion(response
                .getMostSpecificSubdivision()));
        ;
        this.city = (response.getCity() == null ? NOWHERE : makeCity(response.getCity()));
        ;
    }

    private Place makeContinent(final Continent aContinent) {
        return new Place(Place.Type.CONTINENT, aContinent.getName(), aContinent.getCode());
    }

    private Place makeCountry(final Country aCountry) {
        return new Place(Place.Type.COUNTRY, aCountry.getName(), aCountry.getIsoCode());
    }

    private Place makeRegion(final Subdivision aRegion) {
        return new Place(Place.Type.REGION, aRegion.getName(), aRegion.getIsoCode());
    }

    private Place makeCity(final City aCity) {
        final Integer geoNameId = aCity.getGeoNameId();
        if (geoNameId == null)
            return null;

        return new Place(Place.Type.CITY, aCity.getName(), geoNameId.toString());
    }

    public Place getContinent() {
        return continent;
    }

    public Place getCountry() {
        return country;
    }

    public Place getRegion() {
        return region;
    }

    public Place getCity() {
        return city;
    }

    public Match match(Location other) {
        return new Match(this, other);
    }
}
