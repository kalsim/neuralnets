/**
 * Image Wrapper
 *
 * This class contains methods that deal with reading in and manipulating bmp files. It can convert an image array from
 * 2D to 1D and back, and can scale the pels so that they fall between zero and one. This is used so that the image can
 * be inputted into the neural network.
 *
 * Methods in this class:
 * double[] toDoubleArray()
 * void     toGrayScale()
 * void     toBMP(String fileName)
 * int      getHeight()
 * int      getWidth()
 *
 * @author Montek Kalsi
 * @version May 15, 2020
 */
public class ImageWrapper
{

   private final double SCALING_FACTOR = 1 << 24; // The amount to divide all pels by so that they are between 0 and 1

   public int[][] imageArray;                     // The matrix containing all the pels of the image

   /**
    * Creates a new ImageWrapper from a file. This constructor reads the image array from the file and masks off the
    * alpha channel.
    *
    * @param fileName the bmp file containing the image
    */
   public ImageWrapper(String fileName)
   {
      imageArray = DibDump.bmpToArray(fileName);
      for (int i = 0; i < imageArray.length; i++)
      {
         for (int j = 0; j < imageArray[0].length; j++)
         {
            imageArray[i][j] &= 0x00ffffff;
         }
      }
   }

   /**
    * Creates a new ImageWrapper from a one-dimensional array and the height and width of the image. Converts the 1D
    * array to a matrix using the height and width and multiplies it by the scaling factor.
    *
    * @param image  the 1D array containing the pels of the image
    * @param height the height of the image
    * @param width  the width of the image
    */
   public ImageWrapper(double[] image, int height, int width)
   {
      imageArray = new int[height][width];
      for (int i = 0; i < height; i++)
      {
         for (int j = 0; j < width; j++)
         {
            imageArray[i][j] = (int) (image[i * height + j] * SCALING_FACTOR);
         }
      }
   }

   /**
    * Creates a new ImageWrapper from an imageArray. Sets the imageArray instance variable to that imageArray.
    *
    * @param imageArray the 2D array containing the pels of the image
    */
   public ImageWrapper(int[][] imageArray)
   {
      this.imageArray = imageArray;
   }

   /**
    * Converts the imageArray instance variable to a one-dimensional double array and divides by the scaling factor.
    *
    * @return the 1D image array
    */
   public double[] toDoubleArray()
   {
      double[] imageDoubleArray = new double[imageArray.length * imageArray[0].length];

      for (int r = 0; r < imageArray.length; r++)
      {
         for (int c = 0; c < imageArray[0].length; c++)
         {
            imageDoubleArray[r * imageArray.length + c] = (double) (imageArray[r][c]) / SCALING_FACTOR;
         }
      }

      return imageDoubleArray;
   }

   /**
    * Converts the image to gray scale using the grayscale method in DibDump
    */
   public double[] toGrayScale()
   {
      double[] imageDoubleArray = new double[imageArray.length * imageArray[0].length];
      imageArray = DibDump.colorImageToGrayscale(imageArray);

      for (int r = 0; r < imageArray.length; r++)
      {
         for (int c = 0; c < imageArray[0].length; c++)
         {
            RgbQuad rgb = DibDump.pelToRGB(imageArray[r][c]);
            imageDoubleArray[r * imageArray.length + c] = (double)rgb.blue / 255.0;
         }
      }

      return imageDoubleArray;
   }

   /**
    * Takes the imageArray instance variable and puts it in the given bmp file.
    *
    * @param fileName the file in which the image should go
    */
   public void toBMP(String fileName)
   {
      DibDump.imageArrayToBMP(imageArray, fileName);
   }

   /**
    * Gets the height of the image array, or the number of rows
    *
    * @return the height of the image
    */
   public int getHeight()
   {
      return imageArray.length;
   }

   /**
    * Gets the width of the image array, or the number of columns
    *
    * @return the width of the image
    */
   public int getWidth()
   {
      return imageArray[0].length;
   }

}