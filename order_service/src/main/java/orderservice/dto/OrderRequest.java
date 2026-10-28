package orderservice.dto;

//  contiene solo ciò che l'utente ha il permesso di comunicare.
public record OrderRequest( String customerName, String item, Double amount) {

}
