package BeastProject.yahooAPI;

import org.jspace.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class StockStream {
    private static SpaceRepository recommendStockRepository;
    private static RandomSpace evaluatedStockSpace;
    private static SpaceRepository nameRepository;
    private static SequentialSpace names;

    public static void main(String args[]) {
        //Creating a File object for directory
        recommendStockRepository = new SpaceRepository();
        evaluatedStockSpace = new RandomSpace();
        nameRepository = new SpaceRepository();
        names = new SequentialSpace();
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
            StockStream stockStream = new StockStream();
            while (true) {
                try {
                    if (!stockStream.analyzeStockTrend(0)) break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Analyzer 0 was killed");
        });
        Thread analyseStockTrend2 = new Thread(() -> {
            StockStream stockStream = new StockStream();
            while (true) {
                try {
                    if (!stockStream.analyzeStockTrend(1)) break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Analyzer 1 was killed");
        });
        Thread analyseStockTrend3 = new Thread(() -> {
            StockStream stockStream = new StockStream();
            while (true) {
                try {
                    if (!stockStream.analyzeStockTrend(2)) break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Analyzer 2 was killed");
        });
        Thread recommendstocks = new Thread(() -> {
            StockStream stockStream = new StockStream();
            while (true) {
                    if (!stockStream.calculaterecommandations(3)) break;
            }
            System.out.println("The evaluator was killed");
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
        for (int i = 0; i < numOfServices; i++) {
            try {
                nameRepository.get(String.valueOf(i)).put("kill");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            names.put("kill");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean analyzeStockTrend(int threadIdentifier) throws InterruptedException {
        boolean recommend;
        // First we do the query to find the space.
        SequentialSpace sequentialSpace = (SequentialSpace) getNameRepository().get(String.valueOf(threadIdentifier));

        Object[] response = sequentialSpace.getp(new FormalField(String.class));
        if (response != null) {
            if ((response[0]).equals("kill")) {
                return false;
            }
            String stockname = (String) response[0];
            LinkedList<Object[]> objects = sequentialSpace.getAll(new ActualField(stockname),
                    new FormalField(String.class),
                    new FormalField(Double.class),
                    new FormalField(Integer.class));
            double averagechange = 0;
            double max_diff = 0;
            double dailyincrease = 0;
            for (Object[] object : objects) {
                // Analysis of the stock is done here.
            }
            System.out.println(threadIdentifier + " finished analyzing stock: " + stockname);
            recommend = Math.random() > 0.5;
            try {
                getRecommendStockRepository().get(stockname).put(recommend);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public boolean calculaterecommandations(int numOfServices) {
        try {
            String stockrecommandation;
            stockrecommandation = (String) names.query(new FormalField(String.class))[0];
            if (stockrecommandation != null) {
                if (stockrecommandation.equals("kill")) {
                    return false;
                }
                // We do get and then put back and not query because this is a random space. It is a random space because if all the thread doesn't follow the same order
                // This thread might get stuck at a stock which hasn't been evaluated by all the threads yet.

                // If the space is empty we have to do something about, so we have to assign the space to a variable else we can't handle the errror.
                SequentialSpace sequentialSpace = (SequentialSpace) recommendStockRepository.get(stockrecommandation);
                if (sequentialSpace == null) {
                    Thread.sleep(100);
                    calculaterecommandations(numOfServices);
                }
                //Thread.sleep(100);

                List<Object[]> recommandations = recommendStockRepository.get(stockrecommandation).queryAll(new FormalField(Boolean.class));

                if (recommandations.size() == numOfServices) {
                    //recommendStockRepository.get((String) stockrecommandation).getAll(new FormalField(Boolean.class));
                    names.get(new ActualField(stockrecommandation));
                    double i = 0;
                    for (Object[] object : recommandations) {
                        if ((Boolean) object[0]) {
                            i++;
                        }
                    }
                    boolean recommended = i / numOfServices > 0.5;
                    System.out.println("The stock: " + stockrecommandation + " was evaluated as " + recommended);
                    evaluatedStockSpace.put(stockrecommandation, recommended);
                    recommendStockRepository.remove(stockrecommandation);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static SpaceRepository getNameRepository() {
        return nameRepository;
    }

    public static SpaceRepository getRecommendStockRepository() {
        return recommendStockRepository;
    }
}


