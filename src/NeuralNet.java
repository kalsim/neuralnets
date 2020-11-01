import java.io.*;
import java.util.StringTokenizer;

/**
 * This class allows one to construct a neural network with a variable number of activation layers
 * and number of activations per layer. The connectivity pattern is such that every adjacent layer
 * is fully connected.
 *
 * This neural network can be constructed by either passing a file which contains the full weights
 * matrix, or by passing the size of each layer, which will then construct the weights matrix with
 * random values. In addition, these weights can be stored into a file. The network is run by
 * calling the propagate function, which calculates the output of the network from the given input.
 *
 * This network can be trained with any number of training cases. For training, there have to be
 * three layers, with any number of input, hidden, and output nodes.
 *
 * Methods in this class:
 * void     generateWeights()
 * double   uniformRandom(double minVal, double maxVal)
 * void     createActivations()
 * void     storeWeights(String filename)
 * double[] propagate(double[] input)
 * String   train(double[][][] trainingData, double learningRate, double lambdaMult, int epochs)
 * void     backPropagate(double[] input, double[] expected, double learningRate)
 * double   calculateError(double[][][] trainingData)
 * double   outputFunction(double x)
 * double   outputFunctionPrime(double x)
 *
 * @author Montek Kalsi
 * @version May 5, 2020
 */
public class NeuralNet
{
   private int[] sizeOfLayers;            // number of units in each activation layer
   private int numOfLayers;               // number of connectivity layers

   private double[][][] weights;          // weights for connections between each layer
   private double[][] activations;        // state of activation for all processing units

   private double[][] theta;
   private double[][] omega;
   private double[][] psi;

   /**
    * Constructor that creates a neural network with the size of each activation layer given. The
    * first layer is the input layer and the last is the output layer. This constructor generates
    * all weights such that the network is fully connected between adjacent layers. Weights are
    * randomized between -1 and 1.
    *
    * @param sizeOfLayers the number of processing units in each activation layer
    */
   public NeuralNet(int[] sizeOfLayers)
   {
      this.sizeOfLayers = sizeOfLayers;
      numOfLayers = sizeOfLayers.length - 1;

      generateWeights();
      createActivations();
   }

   /**
    * Given the size of each layer and the number of connectivity layers, generates random weights
    * that connect all nodes in each adjacent layer. Weights are randomized between -1 and 1.
    */
   public void generateWeights()
   {
      weights = new double[numOfLayers][][];
      for (int n = 0; n < numOfLayers; n++)
      {
         weights[n] = new double[sizeOfLayers[n]][sizeOfLayers[n + 1]];

         // Generates random numbers for each weight
         for (int i = 0; i < sizeOfLayers[n]; i++)
         {
            for (int j = 0; j < sizeOfLayers[n + 1]; j++)
            {
               weights[n][i][j] = uniformRandom(Main.minWeight, Main.maxWeight);
            }
         }
      }
   }

   /**
    * Generates a random number uniformly between the minimum and maximum value, and returns it
    *
    * @param minVal the minimum value of the random number
    * @param maxVal the maximum value of the random number
    * @return the random number between minVal and maxVal
    */
   private double uniformRandom(double minVal, double maxVal)
   {
      return (Math.random() * (maxVal - minVal)) + minVal;
   }

   /**
    * Constructor that creates a neural network with all the weights given. The weights are assumed
    * to be for connections between each adjacent layer of the network. The weights array has three
    * indices: weights[n][i][j]. n describes what connectivity layer the weight is in, i describes
    * what node in the previous layer the weight is connected to and j describes what node in the
    * next layer the weight is connected to.
    *
    * @param weights the weights of each connection in the network
    */
   public NeuralNet(double[][][] weights)
   {
      this.weights = weights;

      numOfLayers = weights.length;
      sizeOfLayers = new int[weights.length + 1];
      for (int i = 0; i < numOfLayers; i++)
      {
         sizeOfLayers[i] = weights[i].length;
      }
      sizeOfLayers[numOfLayers] = weights[numOfLayers - 1][0].length;

      createActivations();
   }

   /**
    * Constructor that creates a neural net with all the weights given. The format for the weights
    * is as follows: first, the size of each layer is given. Then, for each layer, the matrix for
    * the weights is given. This matrix is such that the number of rows is the number of nodes in
    * the previous activation layer, and the number of columns is the number of nodes in the next
    * activation layer. Each column is space separated and each row is on the next line. There is a
    * blank line between each layer and this format is repeated for each layer.
    *
    * An example of a weights file would be:
    *
    * 2 2 1
    *
    * 0.5 0.5
    * 0.5 0.5
    *
    * 0.3
    * 0.3
    *
    * Here, there are two connectivity layers, shown by the two matrices. The first layer connects
    * two nodes to two nodes, and the second layer connects two nodes to one node.
    *
    * @param filename the name of the file that the weights are stored in
    */
   public NeuralNet(String filename) throws IOException
   {
      BufferedReader br = new BufferedReader(new FileReader(filename));
      String[] splitLine = br.readLine().split(" ");

      // Read in size of each layer
      numOfLayers = splitLine.length - 1;
      sizeOfLayers = new int[splitLine.length];
      for (int i = 0; i < splitLine.length; i++)
      {
         sizeOfLayers[i] = Integer.parseInt(splitLine[i]);
      }

      // Read weights matrix
      weights = new double[numOfLayers][][];
      for (int n = 0; n < numOfLayers; n++)
      {
         weights[n] = new double[sizeOfLayers[n]][sizeOfLayers[n + 1]];
         br.readLine();

         for (int i = 0; i < sizeOfLayers[n]; i++)
         {
            StringTokenizer st = new StringTokenizer(br.readLine());  // Each line is a different row of weights
            for (int j = 0; j < sizeOfLayers[n + 1]; j++)
            {
               weights[n][i][j] = Double.parseDouble(st.nextToken()); // Weights are space-separated on each line
            }
         }
      }

      createActivations();
   }

   /**
    * Creates an empty deltaWeights, activations, theta, omega, and psi array, given that the array sizeOfLayers
    * is already created. If so, they will jagged matrices with each row having a number of columns equal to the
    * size of that layer.
    */
   private void createActivations()
   {
      // These four matrices contain the activation, theta, omega, and psi values for each layer.
      activations = new double[numOfLayers + 1][];
      theta = new double[numOfLayers + 1][];
      omega = new double[numOfLayers + 1][];
      psi = new double[numOfLayers + 1][];

      // Creates a jagged array, with each layer having a length of sizeOfLayers
      for (int n = 0; n < sizeOfLayers.length; n++)
      {
         activations[n] = new double[sizeOfLayers[n]];
         theta[n] = new double[sizeOfLayers[n]];
         omega[n] = new double[sizeOfLayers[n]];
         psi[n] = new double[sizeOfLayers[n]];
      }
   }

   /**
    * Stores the weights in the file given by the filename. It uses the same format to store the
    * weights as when getting the weights from a file, with each layer separated into blocks of
    * weight matrices.
    *
    * @param filename the name of the file to store the weights in
    */
   public void storeWeights(String filename) throws IOException
   {
      PrintWriter pw = new PrintWriter(new FileWriter(filename));

      // Store the size of each layer
      for (int i = 0; i < sizeOfLayers.length; i++)
      {
         pw.print(sizeOfLayers[i] + " ");
      }
      pw.println("\n");

      // Store weights matrix
      for (int n = 0; n < weights.length; n++)
      {
         for (int i = 0; i < weights[n].length; i++)
         {
            for (int j = 0; j < weights[n][0].length; j++)
            {
               pw.print(weights[n][i][j] + " ");
            }
            pw.println();
         }
         pw.println();
      }
      pw.close();
   } // public void storeWeights(String filename)

   /**
    * Given the activations for all input nodes, this function propagates those inputs through the
    * neural net by multiplying each activation layer by the corresponding weights matrix and
    * applying the output function. This is repeated until the values for the output layer are
    * found. This output array is returned.
    *
    * @param input the values for the activation of all input units
    * @return the array of activations for the output units
    */
   public double[] propagate(double[] input)
   {
      activations[0] = input;
      for (int n = 0; n < numOfLayers; n++)
      {
         // calculates the next layer by multiplying the weights by the current layer
         for (int i = 0; i < sizeOfLayers[n + 1]; i++)
         {
            activations[n + 1][i] = 0.0;
            for (int j = 0; j < sizeOfLayers[n]; j++)
            {
               activations[n + 1][i] += weights[n][j][i] * activations[n][j];
            }

            // applies the output function to the nodes
            activations[n + 1][i] = outputFunction(activations[n + 1][i]);
         }
      }

      return activations[numOfLayers];
   } // public double[] propagate(double[] input)

   /**
    * Trains the neural network with the given training data and calculates the error with the test
    * data. The learning rate of the network starts at the given learning rate, and increases or
    * decreases depending on the error. Training runs for a given number of epochs or until the
    * learning rate goes to 0.
    *
    * @param trainingData the inputs and outputs for each training case, used to train the network
    * @param learningRate the initial learning rate of the network
    * @param lambdaMult   how much to multiply the learning rate by for each iteration
    * @param epochs       the number of epochs that training will run for
    */
   public String train(double[][][] trainingData, double learningRate, double lambdaMult, int epochs)
   {
      double minError = Double.MAX_VALUE;

      int e = 1;
      while (e <= epochs && learningRate != 0.0 && minError >= Main.errorThreshold)
      {
         for (double[][] trainingCase : trainingData)
         {
            // Find how much the weights need to change for each training case
            backPropagate(trainingCase[0], trainingCase[1], learningRate);

            // Calculate the error using the training data
            double curError = calculateError(trainingData);

            // Change the learning rate depending on if the error is decreasing or increasing
            if (minError != Double.MAX_VALUE && curError < minError)
            {
               // If the error is decreasing, increase the learning rate
               learningRate *= lambdaMult;
               minError = curError;
            }
            else if (minError != Double.MAX_VALUE && curError >= minError && lambdaMult != 1.0)
            {
               // If the error is increasing, decrease the learning rate
               learningRate /= lambdaMult;
            }
            else
            {
               minError = curError;
            }
         } // for (double[][] trainingCase : trainingData)

         // Print the current error
         if (Main.printingRate != 0 && e % (epochs / Main.printingRate) == 0)
         {
            System.out.println("Epoch " + e + ": Error = " + Math.sqrt(minError));
         }

         e++;
      } // while (e <= epochs && learningRate != 0)

      // Return the ending diagnostic information: the final epoch, learning rate, error, and reason for stopping
      String diagnosticInformation = "";
      diagnosticInformation += "Final Epoch: " + e + "\n";
      diagnosticInformation += "Final Learning Rate: " + learningRate + "\n";
      diagnosticInformation += "Final Error: " + Math.sqrt(minError) + "\n";

      diagnosticInformation += "Reason for stopping: ";
      if (e > epochs)
      {
         diagnosticInformation += "Reached max epochs\n";
      }
      else if (learningRate == 0.0)
      {
         diagnosticInformation += "Learning rate went to 0\n";
      }
      else if (minError < Main.errorThreshold)
      {
         diagnosticInformation += "Reached error threshold\n";
      }

      return diagnosticInformation;
   } // public String train(double[][][] trainingData, double learningRate, double lambdaMult, int epochs)

   /**
    * This is the generalized form of backprop. It finds the gradient of the error function with respect to each weight for a given
    * test case. It can work for a network that has any number of inputs, outputs, or hidden layers.
    *
    * @param input        the input test case to train the network on
    * @param expected     the expected output for that test case
    * @param learningRate the rate at which to change the weights
    */
   private void backPropagate(double[] input, double[] expected, double learningRate)
   {
      // Propagate forward to calculate theta and activations
      activations[0] = input;
      for (int n = 0; n < numOfLayers; n++)
      {
         // Calculates the next layer by multiplying the weights by the current layer
         for (int i = 0; i < sizeOfLayers[n + 1]; i++)
         {
            theta[n + 1][i] = 0.0;
            for (int j = 0; j < sizeOfLayers[n]; j++)
            {
               theta[n + 1][i] += weights[n][j][i] * activations[n][j];
            }

            // Applies the output function to the nodes
            activations[n + 1][i] = outputFunction(theta[n + 1][i]);
         }
      } // for (int n = 0; n < numOfLayers; n++)

      // Calculate omega and psi for the last layer
      for (int i = 0; i < sizeOfLayers[numOfLayers]; i++)
      {
         // omega_i = T_i - a_i
         omega[numOfLayers][i] = expected[i] - activations[numOfLayers][i];

         // psi_i = omega_i * f'(theta_i)
         psi[numOfLayers][i] = omega[numOfLayers][i] * outputFunctionPrime(theta[numOfLayers][i]);
      } // for (int i = 0; i < sizeOfLayers[numOfLayers]; i++)

      // Propagate backwards to calculate omega, psi and weights for everything except the last layer
      for (int n = numOfLayers - 1; n > 0; n--)
      {
         for (int j = 0; j < sizeOfLayers[n]; j++)
         {
            // omega_j = sum of (psi_I * w_jI)
            omega[n][j] = 0.0;
            for (int I = 0; I < sizeOfLayers[n + 1]; I++)
            {
               omega[n][j] += psi[n + 1][I] * weights[n][j][I];
            }

            // psi_j = omega_j * f'(theta_j)
            psi[n][j] = omega[n][j] * outputFunctionPrime(theta[n][j]);

            // deltaWeights_ji = a_j * psi_i
            for (int i = 0; i < sizeOfLayers[n + 1]; i++)
            {
               weights[n][j][i] += learningRate * activations[n][j] * psi[n + 1][i];
            }
         } // for (int j = 0; j < sizeOfLayers[n]; j++)
      } // for (int n = numOfLayers - 1; n > 0; n--)

      // Calculate weights for first layer
      int n = 0;
      for (int i = 0; i < sizeOfLayers[n + 1]; i++)
      {
         for (int j = 0; j < sizeOfLayers[n]; j++)
         {
            // deltaWeights_ji = a_j * psi_i
            weights[n][j][i] += learningRate * activations[n][j] * psi[n + 1][i];
         }
      }
   } // public void backPropagate(double[] input, double[] expected)

   /**
    * Calculates the total error for every single test case in the training data. This total error is a quadratic mean
    * of the error for each test case, which calculates the difference between the output the network gets and the
    * expected output for the input.
    *
    * @param trainingData the inputs and expected output for each training case
    * @return the error between the expected output and the output the network gets
    */
   public double calculateError(double[][][] trainingData)
   {
      double error = 0.0;
      for (double[][] testCase : trainingData)                                                // for each test case
      {
         double[] output = propagate(testCase[0]);                                            // propagate to get the output
         double singleError = 0.0;
         for (int i = 0; i < output.length; i++)
         {
            singleError += 0.5 * (testCase[1][i] - output[i]) * (testCase[1][i] - output[i]); // compare output with expected
         }
         error += singleError * singleError;                                                  // sum this up for each case
      }

      return error;
   }

   /**
    * This is the function used to calculate the output of each activation node.
    *
    * @param x the input for the node
    * @return the function applied to the input
    */
   private double outputFunction(double x)
   {
//      return x;
      return 1.0 / (1.0 + Math.exp(-x));
   }

   /**
    * This returns the derivative of the output function of each activation node evaluated at x.
    *
    * @param x the input for the node
    * @return the derivative of the output function
    */
   private double outputFunctionPrime(double x)
   {
//      return 1.0;
      return outputFunction(x) * (1.0 - outputFunction(x));
   }

} // public class NeuralNet