public class AguaAtrapada {
    public static void main(String[] args) {


        int[] alturas = {0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1};
        int agua = aguaAtrapada(alturas);
        System.out.println("Agua atrapada: " + agua);


    }


    public static int aguaAtrapada(int[] alturas) {

        int izquierda = 0;                       // Índice desde la izquierda
        int derecha = alturas.length - 1;        // Índice desde la derecha
        int maxIzquierda = 0;                    // Altura máxima vista desde la izquierda
        int maxDerecha = 0;                      // Altura máxima vista desde la derecha
        int aguaAtrapada = 0;                    // Total de agua acumulada

        while (izquierda < derecha) {

            if (alturas[izquierda] < alturas[derecha]) {
                
                    if (alturas[izquierda] >= maxIzquierda) {
                        maxIzquierda = alturas[izquierda];
                    } else {
                        aguaAtrapada += maxIzquierda - alturas[izquierda];
                    }

                    izquierda++;

            }else {
                if (alturas[derecha] >= maxDerecha) {
                    maxDerecha = alturas[derecha];
                } else {
                    aguaAtrapada += maxDerecha - alturas[derecha];
                }
                derecha--;
            }

        }

        return aguaAtrapada;
    }
}
