import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Error Minimization
 *
 * Randomizes the weights and trains the neural network on either image training data or manually inputted training
 * data. The user has the option in runtime to choose between images and manually inputted, and they can also change the
 * default file names for all files used.
 *
 * In the configuration file, the user can configure the size of each layer of the neural network, the initial learning
 * rate and learning rate multiplier, the number of epochs that will be run, and the minimum error for the network. In
 * the image training data file, the user gives the filenames of all input and expected bmp files. The training data
 * file contains the number of training cases and the input and output arrays for each training case.  The weights file
 * is where the weights are stored after the neural net completes its training.
 *
 * While running, this class will repeatedly randomize the weights and train the network so as to find a set of weights
 * that lead to a minimum error. After the network is run, if the data was manually inputted, the error and the outputs
 * for each training case is printed. If image data was used, then the output of the network will be put into a bmp file
 * that is given by the user.
 *
 * Methods in this class:
 * void         getConfig(String filename)
 * double[][][] getTrainingData(String filename
 * void         loadImages(String inFileName, String outFileName)
 * void         minimizeBMP()
 * void         minimizeManual()
 * void         main(String[] args)
 *
 * @author Montek Kalsi
 * @version May 15, 2020
 */
public class Main
{
   // the default input files for the program
   static String weightsFile = "weights2.txt";
   static String configFile = "config.txt";
   static String trainingFile = "trainingData.txt";
   static String trainingImageFile = "trainingImageFiles.txt";
   static String trainingImageRawDataFile = "trainingImageRawData.txt";
   static String outputImageFile = "images/output.bmp";

   // meta values that configure the training of the neural net
   static int[] layers;
   static double minWeight;
   static double maxWeight;
   static double learningRate;
   static double lambdaMult;
   static int epochs;
   static int maxIterations;
   static double errorThreshold;
   static int printingRate;
   static int imHeight;
   static int imWidth;

   /**
    * This function reads the configuration of the neural net from the config file. The structure
    * of the config file is as follows:
    *
    * First, the size of each layer is given. Each of these sizes are given space-separated.
    *
    * Each of the next lines contain a variable that configures a part of the training:
    *
    * Min Weight - the smallest value that the weights can be randomized to
    * Max Weight - the largest value that the weights can be randomized to
    * Learning Rate - the initial learning rate of the network
    * Lambda Multiplier - how much to multiply the learning rate by each epoch
    * Epochs - the number of epochs to run
    * Maximum Iterations - the maximum number of times to randomize the weights of the network and retrain it
    * Error Threshold - the neural net stops when it goes below this error
    * Printing Rate - how often to print the error during training
    *
    * @param filename the file to read the configuration from
    */
   static void getConfig(String filename) throws FileNotFoundException
   {
      Scanner sc = new Scanner(new FileReader(filename));

      sc.nextLine();
      String[] line = sc.nextLine().split(" ");

      int numLayers = line.length + 2;
      layers = new int[numLayers];
      for (int i = 1; i < numLayers - 1; i++)
      {
         layers[i] = Integer.parseInt(line[i - 1]);
      }

      sc.next();
      minWeight = sc.nextDouble();

      sc.next();
      maxWeight = sc.nextDouble();

      sc.next();
      learningRate = sc.nextDouble();

      sc.next();
      lambdaMult = sc.nextDouble();

      sc.next();
      epochs = sc.nextInt();

      sc.next();
      maxIterations = sc.nextInt();

      sc.next();
      errorThreshold = sc.nextDouble();
      errorThreshold *= errorThreshold;

      sc.next();
      printingRate = sc.nextInt();
   } // static void getConfig(String filename)

   /**
    * This function reads the training data from a given file, then returns a matrix containing it.
    * This matrix is indexed as trainingData[n][type][i], where n is the test case, type is either 0
    * or 1, 0 for input and 1 for output, and i is the index of the input/output value.
    *
    * The format of the training data is as follows: On the first line, the number of test cases,
    * number of input nodes, number of output nodes are given. Then, on the following lines, for
    * each test case, first the input values are given, space-separated, then the expected output
    * values are given space-separated.
    *
    * An example of a training data file is:
    *
    * 3 2 1
    * 0 0
    * 0
    * 0 1
    * 1
    * 1 0
    * 1
    *
    * In this case, there are 3 test cases, each with 2 input nodes and 1 output node.
    *
    * @param filename the file to read the training data from
    * @return the matrix of training data
    */
   static double[][][] getTrainingData(String filename) throws FileNotFoundException
   {
      Scanner sc = new Scanner(new FileReader(filename));

      // Read sizes
      int sizeOfData = sc.nextInt();
      int sizeOfInput = sc.nextInt();
      int sizeOfOutput = sc.nextInt();

      // Set size of first and last layer
      layers[0] = sizeOfInput;
      layers[layers.length - 1] = sizeOfOutput;

      double[][][] trainingData = new double[sizeOfData][2][];

      for (int i = 0; i < sizeOfData; i++)
      {
         // Read input data
         double[] inputData = new double[sizeOfInput];
         for (int j = 0; j < sizeOfInput; j++)
         {
            inputData[j] = sc.nextDouble();
         }
         trainingData[i][0] = inputData;

         // Read output data
         double[] outputData = new double[sizeOfOutput];
         for (int j = 0; j < sizeOfOutput; j++)
         {
            outputData[j] = sc.nextDouble();
         }
         trainingData[i][1] = outputData;
      }

      return trainingData;
   }

   /**
    * Reads the filenames within the input file, converts those bitmaps to an array and puts that data in the output
    * file. The structure of the input file is as follows: The first line has the number of training cases, the height
    * of each image and the width of each image. Then, the next lines contain the input file and the expected output
    * file. While doing so, the function stores the height and width of the images so they can be converted back later.
    *
    * @param inFileName  the name of the input file containing the bitmaps
    * @param outFileName the name of the output file to print the training data
    */
   static void loadImages(String inFileName, String outFileName) throws FileNotFoundException
   {
      Scanner sc = new Scanner(new FileReader(inFileName));
      PrintWriter pw = new PrintWriter(outFileName);

      // Read sizes
      int sizeOfData = sc.nextInt();
      int sizeOfInput = sc.nextInt();
      int sizeOfOutput = sc.nextInt();
      pw.println(sizeOfData + " " + sizeOfInput + " " + sizeOfOutput);

      for (int i = 0; i < sizeOfData; i++)
      {
         // Read image from given file
         ImageWrapper inImage = new ImageWrapper(sc.next());
         double[] inArray = inImage.toGrayScale();

         // Store the height and width of the image
         imHeight = inImage.getHeight();
         imWidth = inImage.getWidth();

         // Print the image data to the output file
//         double[] inArray = inImage.toDoubleArray();
         for (int j = 0; j < inArray.length; j++)
         {
            pw.print(inArray[j] + " ");
         }
         pw.println();

         // Read output image and print data to the output file
         ImageWrapper outImage = new ImageWrapper(sc.next());
         double[] outArray = outImage.toGrayScale();
         //double[] outArray = outImage.toDoubleArray();
         for (int j = 0; j < outArray.length; j++)
         {
            pw.print(outArray[j] + " ");
         }
         pw.println();
      } // for (int i = 0; i < sizeOfData; i++)

      pw.close();
   } // static void loadImages(String inFileName, String outFileName)

   /**
    * This function will create and train a neural network with given image training data. It will first get the
    * configuration of the network from the config file, load the image data into the training data file, read the
    * training data and input it into the network, then train the network on that data. Finally, it will put the
    * output of the network into a bmp file given by the user.
    */
   static void minimizeBMP() throws IOException
   {
      // Get the configuration of the neural net from the config file
      getConfig(configFile);

      // Read images and load them into the training file
      loadImages(trainingImageFile, trainingImageRawDataFile);

      // Load the training data from the training file
      //System.out.println("Getting Training Data...");
      double[][][] trainingData = getTrainingData(trainingImageRawDataFile);

      // Create a neural net with the given layer sizes
      //System.out.println("Creating Network...");
      NeuralNet nn = new NeuralNet(layers);

      // Train with the given configuration
      //System.out.println("Training...");
      String diagnosticInformation = nn.train(trainingData, learningRate, lambdaMult, epochs);

      nn.storeWeights(weightsFile);
      System.out.println(diagnosticInformation);

      // Create bmp file from output of neural net
      double[] image = nn.propagate(trainingData[0][0]);
      ImageWrapper im = new ImageWrapper(image, imHeight, imWidth);
      im.toGrayScale();
      im.toBMP(outputImageFile);
   }

   /**
    * This function will create and train a neural network with manually inputted training data. This data can have any
    * number of inputs or outputs. The function first gets the configuration of the network, reads the training data,
    * then repeatedly trains the network on that data. It randomizes and trains the network multiple times so as to find
    * the set of weights that lead to the minimum error. When it is done, it prints the error and outputs for each
    * training case.
    */
   static void minimizeManual() throws IOException
   {
      // Get the configuration of the neural net from the config file
      getConfig(configFile);

      // Load the training data from the training file
      System.out.println("Getting Training Data...");
      double[][][] trainingData = getTrainingData(trainingFile);

      // Create a neural net with the given layer sizes
      System.out.println("Creating Network...");
      NeuralNet nn = new NeuralNet(layers);

      System.out.println("Training...");
      double minError = Double.MAX_VALUE;
      int e = 1;
      while (e <= maxIterations && minError > errorThreshold * errorThreshold)
      {
         // Randomize the weights
         nn.generateWeights();

         // Train with the given configuration
         String diagnosticInformation = nn.train(trainingData, learningRate, lambdaMult, epochs);

         // Calculate the error
         double curError = nn.calculateError(trainingData);

         // Print the error and run each of the test cases if error goes down
         if (curError < minError)
         {
            minError = curError;
            nn.storeWeights(weightsFile);
            System.out.println("Iteration " + e);

            System.out.println(diagnosticInformation);

            // For each test case
            for (double[][] testCase : trainingData)
            {
               // Print each input
               StringBuilder printedTestCase = new StringBuilder();
               printedTestCase.append("Input:    ");
               for (int i = 0; i < testCase[0].length; i++)
               {
                  printedTestCase.append(testCase[0][i]).append(",");
               }

               // Print the expected output for the test case
               printedTestCase.deleteCharAt(printedTestCase.length() - 1);
               printedTestCase.append("\nExpected: ");
               for (int i = 0; i < testCase[1].length; i++)
               {
                  printedTestCase.append(testCase[1][i]).append(",");
               }

               // Print the neural network's output for the test case
               printedTestCase.deleteCharAt(printedTestCase.length() - 1);
               printedTestCase.append("\nOutput:   ");
               double[] output = nn.propagate(testCase[0]);
               for (int i = 0; i < output.length; i++)
               {
                  printedTestCase.append(output[i]).append(",");
               }
               printedTestCase.deleteCharAt(printedTestCase.length() - 1);
               System.out.println(printedTestCase + "\n");
            } // for (double[][] testCase : trainingData)
            System.out.println("\n");
         } // if (curError < minError)

         e++;
      } // while (e <= maxIterations && minError > errorThreshold * errorThreshold)
   } // static void minimizeBoolean()

   /**
    * First, it asks the user whether they will be using images or training with manually inputted data. It then asks
    * if the user would like to change any of the default files and asks the user for each file name in turn. Finally,
    * it calls the appropriate function depending the user's answer to the first question.
    */
   public static void main(String[] args) throws IOException
   {
      // If the user wants to override the file paths, they can enter in the config, training data, and weights files manually
      Scanner sc = new Scanner(System.in);

      System.out.println("Will you be manually entering in the training data? (y/n)");
      String type = sc.next();

      System.out.println("Do you want to override the default file paths? (y/n)");
      String override = sc.next();

      if (override.charAt(0) == 'y')
      {
         String defaultResponse = "default";

         System.out.println("What is the file path of the config file? (type " + defaultResponse + " for the default path)");
         override = sc.next();
         if (!override.equals(defaultResponse))
         {
            configFile = override;
         }

         if (type.charAt(0) == 'y')
         {
            System.out.println("What is the file path of the image training data file? (type " + defaultResponse +
                  " for the default path)");
            override = sc.next();
            if (!override.equals(defaultResponse))
            {
               trainingImageFile = override;
            }
         }

         System.out.println("What is the file path of the training data file? (type " + defaultResponse + " for the default path)");
         override = sc.next();
         if (!override.equals(defaultResponse))
         {
            trainingFile = override;
         }

         System.out.println("What is the file path of the weights file? (type " + defaultResponse + " for the default path)");
         override = sc.next();
         if (!override.equals(defaultResponse))
         {
            weightsFile = override;
         }

         if (type.charAt(0) == 'y')
         {
            System.out.println("What is the file path of the output image file? (type " + defaultResponse +
                  " for the default path)\nIt should be a bmp file.");
            override = sc.next();
            if (!override.equals(defaultResponse))
            {
               outputImageFile = override;
            }
         }
      } // if (override.charAt(0) == 'y')

      if (type.charAt(0) == 'n')
      {
         minimizeBMP();       // Should load images then minimize
      }
      else
      {
         minimizeManual();   // Should minimize using manually inputted training data
      }
   } // public static void main(String[] args)

}