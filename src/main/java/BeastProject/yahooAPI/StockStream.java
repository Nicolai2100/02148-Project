package BeastProject.yahooAPI;

import org.jspace.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class StockStream {
    private static SpaceRepository recommendStockRepository;
    private static RandomSpace evaluatedStockSpace;
    private static SpaceRepository nameRepository;
    private static RandomSpace names;

    public static void main(String args[]) {
        //Creating a File object for directory
        recommendStockRepository = new SpaceRepository();
        evaluatedStockSpace = new RandomSpace();
        nameRepository = new SpaceRepository();
        names = new RandomSpace();
        int numofservices = 3;
        // One for each analyzing service.
        for (int i = 0; i < 3; i++) {
            getNameRepository().add(String.valueOf(i), new SequentialSpace());
        }

        Thread generateFiles = new Thread(() -> {
            try {
                StockStream stockStream = new StockStream();
                stockStream.generateFileRepository(numofservices);
                System.out.println("All files added, ending thread");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        Thread analyseStockTrend1 = new Thread(() -> {
            while (true) {
                StockStream stockStream = new StockStream();
                try {
                    stockStream.analyzeStockTrend(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread analyseStockTrend2 = new Thread(() -> {
            while (true) {
                StockStream stockStream = new StockStream();
                try {
                    stockStream.analyzeStockTrend(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread analyseStockTrend3 = new Thread(() -> {
            while (true) {
                StockStream stockStream = new StockStream();
                try {
                    stockStream.analyzeStockTrend(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread recommendstocks = new Thread(() -> {
            while (true) {
                StockStream stockStream = new StockStream();
                stockStream.calculaterecommandations(numofservices);
            }
        });
        generateFiles.start();
        analyseStockTrend1.start();
        analyseStockTrend2.start();
        analyseStockTrend3.start();
        recommendstocks.start();
    }

    public void generateFileRepository(int numOfServices) throws IOException {
        File directoryPath = new File("txtStockFiles");
        //List of all files and directories
        File filesList[] = directoryPath.listFiles();
        // Intialize the repository containing a space for each of the threads running
        // Loop over the files
        for (File file : filesList) {
            // When the file is too long this just gives up and dies
            BufferedReader reader;
            reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
            reader.readLine();
            String line;
            int linesread = 0;
            while ((line = reader.readLine()) != null && linesread < 1000) {
                linesread++;
                String[] commasperated = line.split(",");
                try {
                    // Tuple = Dato, start value, number of stocks, might also have to include name;
                    Object[] object = new Object[4];
                    object[0] = file.getName();
                    object[1] = commasperated[0];
                    object[2] = Double.valueOf(commasperated[1]);
                    object[3] = Integer.valueOf(commasperated[5]);
                    // Now we add a copy to each space.
                    for (int i = 0; i < numOfServices; i++) {
                        nameRepository.get(String.valueOf(i)).put(object);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Stock added: " + file.getName());
            recommendStockRepository.add(file.getName(), new SequentialSpace());
            try {
                names.put(file.getName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // The files are added to each of the repository when all the tuples are uploaded, else it might happen that the thread only collects some of the tuples.
            for (int i = 0; i < numOfServices; i++) {
                try {
                    nameRepository.get(String.valueOf(i)).put(file.getName());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void analyzeStockTrend(int threadIdentifier) throws InterruptedException {
        boolean recommend;
        // First we do the query to find the space.
        SequentialSpace sequentialSpace = (SequentialSpace) getNameRepository().get(String.valueOf(threadIdentifier));

        Object[] response = sequentialSpace.getp(new FormalField(String.class));
        if (response != null) {
            String stockname = (String) response[0];
            LinkedList<Object[]> objects = sequentialSpace.getAll(new ActualField(stockname),
                    new FormalField(String.class),
                    new FormalField(Double.class),
                    new FormalField(Integer.class));
            for (Object[] object : objects) {
                // Analysis of the stock is done here.
                int j = 0;
            }
            System.out.println(threadIdentifier + " finished analyzing stock: " + stockname);
            recommend = Math.random() > 0.5;
            try {
                getRecommendStockRepository().get(stockname).put(recommend);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void calculaterecommandations(int numOfServices) {
        try {
            Object stockrecommandation = names.query(new FormalField(String.class))[0];
            // We do get and then put back and not query because this is a random space. It is a random space because if all the thread doesn't follow the same order
            // This thread might get stuck at a stock which hasn't been evaluated by all the threads yet.

            // If the space is empty we have to do something about, so we have to assign the space to a variable else we can't handle the errror.
            SequentialSpace sequentialSpace = (SequentialSpace) recommendStockRepository.get((String) stockrecommandation);
            if (sequentialSpace == null) {
                Thread.sleep(100);
                calculaterecommandations(numOfServices);
            }

            List<Object[]> recommandations = recommendStockRepository.get((String) stockrecommandation).queryAll(new FormalField(Boolean.class));

            if (recommandations.size() == numOfServices) {
                recommendStockRepository.get((String) stockrecommandation).getAll(new FormalField(Boolean.class));
                int i = 0;
                for (Object[] object : recommandations) {
                    if ((Boolean) object[0]) {
                        i++;
                    }
                }
                boolean recommended = i % numOfServices > 0;
                System.out.println("The stock: " + stockrecommandation + " was evaluated as " + recommended);
                evaluatedStockSpace.put(stockrecommandation, recommended);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static SpaceRepository getNameRepository() {
        return nameRepository;
    }

    public static SpaceRepository getRecommendStockRepository() {
        return recommendStockRepository;
    }
}


