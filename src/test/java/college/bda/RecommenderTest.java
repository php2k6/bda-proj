package college.bda;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static college.bda.ModelData.*;

class RecommenderTest {
    ModelData fixture() {
        ModelData m = new ModelData();m.rank=2;m.regParam=.1;m.globalMean=3;
        m.movies.addAll(List.of(new Movie(1,"One","Action",30,4,3.8),new Movie(2,"Two","Drama",25,3,3.1),
                new Movie(3,"Three","Action|Drama",40,4.5,4),new Movie(4,"Four","Comedy",1,5,3.01)));
        m.items.put(1,new double[]{1,0});m.items.put(2,new double[]{0,1});m.items.put(3,new double[]{1,1});m.items.put(4,new double[]{2,1});
        m.users.put(1,new double[]{1,.5});m.histories.put(1,List.of(new Rating(1,5)));
        return m;
    }
    @Test void excludesRatedMovies() {assertFalse(new Recommender(fixture()).forUser(1,10,"",0).stream().anyMatch(m->m.movieId()==1));}
    @Test void ranksByDotProduct() {assertEquals(3,new Recommender(fixture()).forUser(1,2,"",20).get(0).movieId());}
    @Test void filtersMinimumSupport() {assertTrue(new Recommender(fixture()).forUser(1,10,"",20).stream().allMatch(m->m.ratingCount()>=20));}
    @Test void appliesExactGenre() {assertEquals(1,new Recommender(fixture()).forUser(1,10,"Action",20).size());}
    @Test void unknownUserFallsBackToWeightedPopularity() {assertEquals(3,new Recommender(fixture()).forUser(999,1,"",20).get(0).movieId());}
    @Test void supportsEmptyFilteredResult() {assertTrue(new Recommender(fixture()).forUser(1,10,"Horror",0).isEmpty());}
    @Test void rejectsEmptyProfile() {assertThrows(IllegalArgumentException.class,()->new Recommender(fixture()).forProfile(List.of(),10,"",0));}
    @Test void rejectsDuplicateProfileMovies() {assertThrows(IllegalArgumentException.class,()->new Recommender(fixture()).forProfile(List.of(new Rating(1,3),new Rating(1,4)),10,"",0));}
    @Test void rejectsNonHalfStarRating() {assertThrows(IllegalArgumentException.class,()->new Recommender(fixture()).forProfile(List.of(new Rating(1,4.2)),10,"",0));}
    @Test void rejectsNonFiniteRating() {assertThrows(IllegalArgumentException.class,()->new Recommender(fixture()).forProfile(List.of(new Rating(1,Double.NaN)),10,"",0));}
    @Test void rejectsUnknownMovie() {assertThrows(IllegalArgumentException.class,()->new Recommender(fixture()).forProfile(List.of(new Rating(99,4)),10,"",0));}
    @Test void newProfileExcludesItsInputs() {assertTrue(new Recommender(fixture()).forProfile(List.of(new Rating(1,4),new Rating(2,3)),10,"",0).stream().noneMatch(m->m.movieId()==1||m.movieId()==2));}
    @Test void regularizedProfileMatchesSmallAnalyticSolution() {assertArrayEquals(new double[]{4/1.1,0},new Recommender(fixture()).fitProfile(List.of(new Rating(1,4))),1e-8);}
    @Test void searchIsCaseInsensitiveAndHandlesCommas() {assertEquals("Three",new Recommender(fixture()).search("THR",10).get(0).title());}
    @Test void rejectsInvalidLimit() {assertThrows(IllegalArgumentException.class,()->new Recommender(fixture()).forUser(1,100,"",0));}
    @Test void profileDoesNotChangeStoredHistory() {ModelData m=fixture();new Recommender(m).forProfile(List.of(new Rating(1,3)),3,"",0);assertEquals(5,m.histories.get(1).get(0).rating());}
}
