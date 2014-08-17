package com.workshare.msnos.core.geo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.maxmind.geoip2.model.OmniResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Continent;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Subdivision;
import com.workshare.msnos.core.geo.Location.Match;
import com.workshare.msnos.core.geo.Location.Place;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OmniResponse.class, Continent.class, City.class, Country.class, Subdivision.class})
public class LocationTest {

    public static final Continent NORTH_AMERICA = mockContinent("NA", "North America");
    public static final Continent EUROPE = mockContinent("EU", "Europe");
    
    public static final Country UNITED_STATES = mockCountry("US", "United States");
    public static final Country CANADA = mockCountry("CA", "Canada");
        
    public static final Subdivision NEW_YORK = mockRegion("NY","New York");
    
    
    public static final City SYRACUSE = mock(City.class);
    static {
        when(SYRACUSE.getGeoNameId()).thenReturn(5116079);
        when(SYRACUSE.getName()).thenReturn("Syracuse");
    }

    static {
        when(EUROPE.getCode()).thenReturn("EU");
        when(EUROPE.getName()).thenReturn("EUROPE");
    }

    

    @Before
    public void setup() {
    }

    @Test
    public void shouldCreateContinentRecord() {
        Location location = new Location (response(NORTH_AMERICA));
        
        assertEquals(Place.Type.CONTINENT, location.getContinent().getType());
        assertEquals(NORTH_AMERICA.getCode(), location.getContinent().getCode());
        assertEquals(NORTH_AMERICA.getName(), location.getContinent().getName());
    }

    @Test
    public void shouldCreateCountryRecord() {
        Location location = new Location (response(UNITED_STATES));
        
        assertEquals(Place.Type.COUNTRY, location.getCountry().getType());
        assertEquals(UNITED_STATES.getIsoCode(), location.getCountry().getCode());
        assertEquals(UNITED_STATES.getName(), location.getCountry().getName());
    }

    @Test
    public void shouldCreateRegionRecord() {
        Location location = new Location (response(NEW_YORK));
        
        assertEquals(Place.Type.REGION, location.getRegion().getType());
        assertEquals(NEW_YORK.getIsoCode(), location.getRegion().getCode());
        assertEquals(NEW_YORK.getName(), location.getRegion().getName());
    }

    @Test
    public void shouldCreateCityRecord() {
        Location location = new Location (response(SYRACUSE));
        
        assertEquals(Place.Type.CITY, location.getCity().getType());
        assertEquals(SYRACUSE.getGeoNameId().toString(), location.getCity().getCode());
        assertEquals(SYRACUSE.getName(), location.getCity().getName());
    }

    @Test
    public void shouldNotMatchWithNowhere() {
        Location america = new Location (response(NORTH_AMERICA));
        Location nowhere = new Location (response());
        
        Match match = america.match(nowhere);
        
        assertEquals(0, match.value());
    }
    
    @Test
    public void shouldNotMatchIfContinentDifferent() {
        Location america = new Location (response(NORTH_AMERICA));
        Location europe = new Location (response(EUROPE));
        
        Match match = america.match(europe);
        
        assertEquals(0, match.value());
    }
    
    @Test
    public void shouldMatchByContinent() {
        Location self = new Location (response(NORTH_AMERICA, null, null, null));
        Location other = new Location (response(NORTH_AMERICA, UNITED_STATES, null, null));
        
        Match match = self.match(other);
        
        assertEquals(1, match.value());
    }
    
    @Test
    public void shouldMatchByCountry() {
        Location self = new Location (response(NORTH_AMERICA, UNITED_STATES, null, null));
        Location other = new Location (response(NORTH_AMERICA, UNITED_STATES, NEW_YORK, null));
        
        Match match = self.match(other);
        
        assertEquals(3, match.value());
    }
    
    @Test
    public void shouldMatchByRegion() {
        Location self = new Location (response(NORTH_AMERICA, UNITED_STATES, NEW_YORK, null));
        Location other = new Location (response(NORTH_AMERICA, UNITED_STATES, NEW_YORK, SYRACUSE));
        
        Match match = self.match(other);
        
        assertEquals(7, match.value());
    }
    
    @Test
    public void shouldMatchByCity() {
        Location self = new Location (response(NORTH_AMERICA, UNITED_STATES, NEW_YORK, SYRACUSE));
        Location other = new Location (response(NORTH_AMERICA, UNITED_STATES, NEW_YORK, SYRACUSE));
        
        Match match = self.match(other);
        
        assertEquals(15, match.value());
    }
    
    @Test
    public void shouldCalculatePrecisionWhenNowhere() {
        assertEquals(0, Location.UNKNOWN.getPrecision());
    }
    
    @Test
    public void shouldCalculatePrecisionCorrectly() {
        Location continent = new Location (response(NORTH_AMERICA, null, null, null));
        Location country = new Location (response(NORTH_AMERICA, UNITED_STATES, null, null));
        Location region = new Location (response(NORTH_AMERICA, UNITED_STATES, NEW_YORK, null));
        Location city = new Location (response(NORTH_AMERICA, UNITED_STATES, NEW_YORK, SYRACUSE));
        
        assertTrue(country.getPrecision() > continent.getPrecision());
        assertTrue(region.getPrecision() > country.getPrecision());
        assertTrue(city.getPrecision() > region.getPrecision());
        
    }
    
    @Test
    public void shouldComputeMostPreciseLocationAsNowherWhenNoNetworks() {
        Location location = Location.computeMostPreciseLocation(null);
        assertEquals(Location.UNKNOWN, location);
    }
    
    @Test
    public void shouldNotMatchWhenOtherIsNull() {
        Location self = new Location (response(NORTH_AMERICA, UNITED_STATES, NEW_YORK, SYRACUSE));
        Match match = self.match(null);
        assertEquals(0, match.value());
    }
    
    @Test
    public void shouldToStringNicelyGoodLocation() {
        Location loc = new Location (response(NORTH_AMERICA, UNITED_STATES, NEW_YORK, SYRACUSE));
        assertEquals("{\"location\":\"North America, United States, New York, Syracuse\", \"precision\":15}", loc.toString());
    }
    
    @Test
    public void shouldToStringNicelyDecentLocation() {
        Location loc = new Location (response(NORTH_AMERICA, UNITED_STATES, null, null));
        assertEquals("{\"location\":\"North America, United States\", \"precision\":3}", loc.toString());
    }
    
    @Test
    public void shouldToStringNicelyUnknownLocation() {
        Location loc = Location.UNKNOWN;
        assertEquals("{\"location\":\"unknown\", \"precision\":0}", loc.toString());
    }
    
    private OmniResponse response(Continent continent) {
        return response(continent, null, null, null);
    }
    
    private OmniResponse response(Country country) {
        return response(null, country, null, null);
    }
    
    private OmniResponse response(Subdivision region) {
        return response(null, null, region, null);
    }
    
    private OmniResponse response(City city) {
        return response(null, null, null, city);
    }
    
    private OmniResponse response() {
        return response(null, null, null, null);
    }
    
    private OmniResponse response(Continent continent, Country country, Subdivision region, City city) {
        OmniResponse response = mock(OmniResponse.class);
        when(response.getContinent()).thenReturn(continent );
        when(response.getCountry()).thenReturn(country);
        when(response.getMostSpecificSubdivision()).thenReturn(region);
        when(response.getCity()).thenReturn(city);
        return response;
    }
    
    private static Continent mockContinent(String code, String name) {
        Continent continent = mock(Continent.class);
        when(continent.getCode()).thenReturn(code);
        when(continent.getName()).thenReturn(name);
        return continent;
    }

    private static Country mockCountry(String code, String name) {
        Country country = mock(Country.class);
        when(country.getIsoCode()).thenReturn(code);
        when(country.getName()).thenReturn(name);
        return country;
    }
    
    private static final Subdivision mockRegion(String code, String name) {
        Subdivision region = mock(Subdivision.class);
        when(region.getIsoCode()).thenReturn(code);
        when(region.getName()).thenReturn(name);
        return region;
    }
}
