import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class AGtsp {

    private ArrayList<Cidade> cidades = new ArrayList<>();
    private int tamPopulacao;
    private double probMutacao;
    private int qtdeCruzamentos;
    private int numeroGeracoes;
    private ArrayList<ArrayList<Cidade>> populacao = new ArrayList<>();
    private ArrayList<Integer> roletaVirtual = new ArrayList<>();
    private Random rand = new Random();

    public AGtsp(int tamPopulacao, int probMutacao, int qtdeCruzamentos, int numeroGeracoes) {
        this.tamPopulacao = tamPopulacao;
        this.probMutacao = probMutacao;
        this.qtdeCruzamentos = qtdeCruzamentos;
        this.numeroGeracoes = numeroGeracoes;
    }

    public void executar() {
        criarPopulacao();
        gerarRoleta(); // Gera a roleta para a população inicial

        // Mostra a evolução da população a cada geração [cite: 44]
        for (int i = 0; i < this.numeroGeracoes; i++) {
            novaPopulacao(); // Aplica seleção, crossover e mutação
            System.out.println("Geração " + (i + 1) + ": Melhor distância = " + String.format("%.2f", (1.0 / fitness(populacao.get(obterMelhor())))));
        }

        int melhor = obterMelhor();
        System.out.println("\nMelhor solução encontrada:"); // [cite: 47]
        mostrarRota(populacao.get(melhor));
    }

    public void carregarCidades(String arquivo) {
        String linha;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(arquivo), "UTF-8"))) {
            while ((linha = br.readLine()) != null) {
                String[] dados = linha.split(",");
                String nome = dados[0].trim();
                double x = Double.parseDouble(dados[1]);
                double y = Double.parseDouble(dados[2]);
                Cidade cidade = new Cidade(nome, x, y);
                cidades.add(cidade);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Um cromossomo é uma permutação de cidades (uma rota) [cite: 17]
    private ArrayList<Cidade> criarCromossomo() {
        ArrayList<Cidade> cromossomo = new ArrayList<>(this.cidades);
        Collections.shuffle(cromossomo);
        return cromossomo;
    }

    private void criarPopulacao() {
        for (int i = 0; i < this.tamPopulacao; i++) {
            this.populacao.add(criarCromossomo());
        }
    }

    private double calcularDistancia(Cidade a, Cidade b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Calcula o fitness como o inverso da distância total [cite: 20]
    private double fitness(ArrayList<Cidade> cromossomo) {
        double distanciaTotal = 0;

        // Soma as distâncias entre cidades consecutivas na rota
        for (int i = 0; i < cromossomo.size() - 1; i++) {
            distanciaTotal += calcularDistancia(cromossomo.get(i), cromossomo.get(i + 1));
        }
        // Adiciona a distância da última cidade de volta para a primeira
        distanciaTotal += calcularDistancia(cromossomo.get(cromossomo.size() - 1), cromossomo.get(0));

        return 1.0 / distanciaTotal;
    }

    private void gerarRoleta() {
        this.roletaVirtual.clear();
        double fitnessTotal = 0;
        for (ArrayList<Cidade> individuo : this.populacao) {
            fitnessTotal += fitness(individuo);
        }

        for (int i = 0; i < this.populacao.size(); i++) {
            double fitnessRelativo = fitness(this.populacao.get(i)) / fitnessTotal;
            int qtdeSlots = (int) (fitnessRelativo * 10000); // Escala para melhor precisão
            for (int j = 0; j < qtdeSlots; j++) {
                this.roletaVirtual.add(i); // Adiciona o índice do indivíduo na roleta
            }
        }
    }

    private int roleta() {
        if (this.roletaVirtual.isEmpty()) {
            return rand.nextInt(this.populacao.size());
        }
        int indiceSorteado = rand.nextInt(this.roletaVirtual.size());
        return this.roletaVirtual.get(indiceSorteado);
    }

    // Cruzamento PMX (Partially Mapped Crossover) [cite: 23]
    public ArrayList<ArrayList<Cidade>> cruzamentoPMX(ArrayList<Cidade> pai1, ArrayList<Cidade> pai2) {
        int tamanho = pai1.size();

        int corte1 = rand.nextInt(tamanho);
        int corte2 = rand.nextInt(tamanho);
        if (corte1 > corte2) {
            int temp = corte1;
            corte1 = corte2;
            corte2 = temp;
        }

        ArrayList<Cidade> filho1 = new ArrayList<>(Collections.nCopies(tamanho, null));
        ArrayList<Cidade> filho2 = new ArrayList<>(Collections.nCopies(tamanho, null));

        for (int i = corte1; i <= corte2; i++) {
            filho1.set(i, pai2.get(i));
            filho2.set(i, pai1.get(i));
        }

        preencherPMX(filho1, pai1, pai2, corte1, corte2);
        preencherPMX(filho2, pai2, pai1, corte1, corte2);

        ArrayList<ArrayList<Cidade>> filhos = new ArrayList<>();
        filhos.add(filho1);
        filhos.add(filho2);
        return filhos;
    }

    private void preencherPMX(ArrayList<Cidade> filho, ArrayList<Cidade> paiOrigem, ArrayList<Cidade> paiSegmento, int corte1, int corte2) {
        for (int i = 0; i < paiOrigem.size(); i++) {
            if (i >= corte1 && i <= corte2) continue;

            Cidade geneAInserir = paiOrigem.get(i);
            while (filho.contains(geneAInserir)) {
                int indexNoFilho = filho.indexOf(geneAInserir);
                geneAInserir = paiOrigem.get(indexNoFilho);
            }
            filho.set(i, geneAInserir);
        }
    }

    // Mutação por troca de duas posições (swap) [cite: 38]
    private void mutacao(ArrayList<Cidade> cromossomo) {
        if (rand.nextDouble() * 100 < this.probMutacao) {
            int pos1 = rand.nextInt(cromossomo.size());
            int pos2 = rand.nextInt(cromossomo.size());
            while (pos1 == pos2) {
                pos2 = rand.nextInt(cromossomo.size());
            }
            Collections.swap(cromossomo, pos1, pos2);
        }
    }

    private int obterMelhor() {
        int melhor = 0;
        double melhorFitness = 0.0;
        for (int i = 0; i < this.populacao.size(); i++) {
            double f = fitness(this.populacao.get(i));
            if (f > melhorFitness) {
                melhorFitness = f;
                melhor = i;
            }
        }
        return melhor;
    }

    private void novaPopulacao() {
        ArrayList<ArrayList<Cidade>> novaPopulacao = new ArrayList<>();

        // 1. Elitismo: Adiciona o melhor indivíduo da população atual na nova
        novaPopulacao.add(new ArrayList<>(this.populacao.get(obterMelhor())));

        // 2. Crossover: Realiza a quantidade de cruzamentos definida
        for (int i = 0; i < this.qtdeCruzamentos; i++) {
             if (novaPopulacao.size() >= this.tamPopulacao) break;

            int indicePai1 = roleta();
            int indicePai2 = roleta();
            while (indicePai1 == indicePai2) {
                indicePai2 = roleta();
            }

            ArrayList<Cidade> pai1 = this.populacao.get(indicePai1);
            ArrayList<Cidade> pai2 = this.populacao.get(indicePai2);
            ArrayList<ArrayList<Cidade>> filhos = cruzamentoPMX(pai1, pai2);

            mutacao(filhos.get(0));
            mutacao(filhos.get(1));

            novaPopulacao.add(filhos.get(0));
             if (novaPopulacao.size() < this.tamPopulacao) {
                novaPopulacao.add(filhos.get(1));
            }
        }

        // 3. Reprodução: Preenche o resto com os melhores da população anterior
        while (novaPopulacao.size() < this.tamPopulacao) {
            int indice = roleta();
            novaPopulacao.add(new ArrayList<>(this.populacao.get(indice)));
        }

        this.populacao = novaPopulacao;
        gerarRoleta(); // Atualiza a roleta para a nova população
    }

    // Exibe a rota final e a distância total [cite: 41, 43]
    private void mostrarRota(ArrayList<Cidade> rota) {
        System.out.println("Rota:"); // [cite: 48]
        for (int i = 0; i < rota.size(); i++) {
            System.out.print(rota.get(i).getNome() + " -> ");
        }
        System.out.println(rota.get(0).getNome());

        double distanciaTotal = 1.0 / fitness(rota);
        System.out.println("Distância total: " + String.format("%.2f", distanciaTotal));
    }

    public static void main(String[] args) {
        // (população, % mutação, nº de cruzamentos por geração, nº de gerações)
        AGtsp ag = new AGtsp(100, 5, 40, 200);
        ag.carregarCidades("cidades.csv");
        ag.executar();
    }
}