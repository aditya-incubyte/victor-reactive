package victor.training.reactive.blocking;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

import static java.lang.System.currentTimeMillis;
import static victor.training.reactive.blocking.Utils.sleep;

@EnableAsync
@RestController
@SpringBootApplication
public class BlockingApp {

   private static final Logger log = LoggerFactory.getLogger(BlockingApp.class);

   @Bean
   MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
      return registry -> registry.config()
          .commonTags(
              "application", "spring-mvc",
              "region", "training-region");
   }




   public static void main(String[] args) {
      org.springframework.boot.SpringApplication.run(BlockingApp.class, args);
   }

   @Autowired
   private Barman barman;

   @GetMapping("fast")
   public String fast() throws Exception {
      return "immediate";
   }

   @GetMapping("drink")
   public CompletableFuture<DillyDilly> drink() throws Exception {
      log.info("Talking to proxied barman: " + barman.getClass());

      long t0 = currentTimeMillis();


      CompletableFuture<Beer> beerCF = barman.pourBeer();
      CompletableFuture<Vodka> vodkaCF = barman.pourVodka();

      CompletableFuture<DillyDilly> futureDilly = beerCF
          .thenCombineAsync(vodkaCF, (b, v) -> new DillyDilly(b, v)/*, anotherThreadPool*/);

      log.debug("Time= " + (currentTimeMillis() - t0));



      return futureDilly;
   }

//   public void hardCore(HttpServletRequest request) throws Exception {
//      AsyncContext asyncContext = request.startAsync();
//      drink().thenAccept(dilly -> {
//         asyncContext.getResponse().getWriter().write("Stuff " + dilly);
//         asyncContext.complete();
//      });
//   }
}

@Service
class Barman {
   private static final Logger log = LoggerFactory.getLogger(Barman.class);

   @Async
   public CompletableFuture<Beer> pourBeer() {
      log.info("Start pour beer");

//      return new AsyncRestTemplate().exchange()
//          .completable().thenApply(tranform);

      sleep(1000); // imagine blocking REST call
      log.info("End pour beer");
      return CompletableFuture.completedFuture(new Beer());
   }

   @Async
   public CompletableFuture<Vodka> pourVodka() {
      log.info("Start pour vodka");
      sleep(200);  // imagine blocking DB call
      log.info("End pour vodka");
      return CompletableFuture.completedFuture(new Vodka());
   }
}


class Beer {
   private final String type = "blond";

   public String getType() {
      return type;
   }
}

class Vodka {
   private final String type = "deadly";

   public String getType() {
      return type;
   }
}

class DillyDilly {
   private static final Logger log = LoggerFactory.getLogger(DillyDilly.class);
   private final Beer beer;
   private final Vodka vodka;

   public DillyDilly(Beer beer, Vodka vodka) {
      this.beer = beer;
      this.vodka = vodka;
      log.info("Mixing {} with {} (takes time) ...", beer, vodka);
      sleep(500);
   }

   public Beer getBeer() {
      return beer;
   }

   public Vodka getVodka() {
      return vodka;
   }
}

class Utils {
   public static void sleep(long millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }
}