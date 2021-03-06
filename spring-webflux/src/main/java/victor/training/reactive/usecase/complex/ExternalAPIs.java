package victor.training.reactive.usecase.complex;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
class ExternalAPIs {

   @SneakyThrows
   public static Product fetchSingleProductDetails(Long productId) {
      log.info("Calling Get Product Details REST");
      RestTemplate rest = new RestTemplate();
      ProductDetailsResponse dto = rest.getForObject("http://localhost:9999/api/product/", ProductDetailsResponse.class, productId);
      return dto.toEntity();
   }

   @SneakyThrows
   public static Mono<Void> auditResealedProduct(Product product) {
      // TODO only audit resealed products !
      return WebClient.create().get().uri("http://localhost:9999/api/audit-resealed/" + product)
          .retrieve()
          .toBodilessEntity()
          .doOnSubscribe(s -> log.info("Calling Audit REST"))
          .then();
   }

   @SneakyThrows
   public static Mono<ProductRatingResponse> fetchProductRating(long productId) {

      if (Math.random() < .5) {
         log.error("Failing rating");
         return Mono.error(new RuntimeException("Oups"));
      }

      log.info("Calling Rating REST is a LIE! - executing this method does NOT do any network > it just creates a Mono");
      return WebClient.create().get().uri("http://localhost:9999/api/rating/{}", productId)
          .retrieve()
          .bodyToMono(ProductRatingResponse.class)
          .doOnSubscribe(s -> log.info("Really Calling Rating REST"))
          ;
   }


}
