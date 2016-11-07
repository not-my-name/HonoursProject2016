
package za.redbridge.simulator;

/*
a class that is used to write experiment details
to text files

declared final to make class static
*/

public final class TheStaticRecorder {

    private String folderPath; //the destination folder to write to
    private String fileName; //variable to hold the name of the file to be written to / gets appended to the folder path
    private String fileHeader; //string to be printed at the beginning of each new file

    //inner class to record the output string and
    //the associated heading to be written to the file
    private class OutputItem {

        private String outputHeading;
        private double doubleValue = -1;

        public OutputItem(String outputHeading, double doubleValue) {
            this.outputHeading = outputHeading;
            this.doubleValue = doubleValue;
        }

        public String getHeading() {
            return this.outputHeading;
        }

        public double getValue() {
            return doubleValue;
        }

    }

    ArrayList<OutputItem> outputBuffer = new ArrayList<OutputItem>();


    /*a private constructor to ensure no instantiation of static class
    a constructor to set the default file path
    can be changed to be used with different file hierarchies */
    private TheStaticRecorder() {

        //temporary folder being used for experimental results
        //can be used to create subfolders or add individual files
        this.folderPath = "/home/ruben/HonoursProject_MAIN/casairt-simulator-master/results/Experimental_Results";
        this.fileName = "/Default_Output.txt"; //the default folder that gets written to without being reset
        this.fileHeader = "Default File Header";
    }

    //method to reset the recorder's details
    private static void initRecorder(String folderPath, String fileName) {

        outputBuffer.clear();
        this.folderPath = folderPath;
        this.fileName = fileName;
    }

    /*
    method to write the currently stored information
    to the file name, either creating a new file or overwriting
    the existing one */
    private static void pushToFile() throws IOException {

        //checking if there are any items to output
        if(outputBuffer.size() > 0) {

            //checking if the current file path exists
            String filePath = folderPath + fileName;
            BufferedWriter buffWriter = null;

            try {

                buffWriter = new BufferedWriter(new FileWriter(filePath, false)); //creating the new buffered writer
                //iterating over each of the items that have been listed for saving
                buffWriter.write(fileHeader);
                buffWriter.newLine();
                for(OutputItem outItem : outputBuffer) {
                    buffWriter.write("%-40s%s%n", outItem.getHeading(), outItem.getValue());
                    buffWriter.newLine();
                }
                //closing the current writer
                buffWriter.flush();
                buffWriter.close();
            }
            catch (IOException error) { //catching a thrown error
                error.printStackTrace();
            }
        }
        else{
            System.out.println("Error: No items to write to file");
            return;
        }
    }

    /*
    method to check if the given file name exists
    if it does exist, write the given/stored information to
    the current filepath
    if it does not exist, create the new file and then write to it,
    returning a notification that file not found*/
    private static void appendToFile() {

        BufferedWriter buffWriter = null;
        String outputPath = this.folderPath + this.fileName; //the final combined output path

        if (outputBuffer.size() > 0) {

            try {

                buffWriter = new BufferedWriter(new FileWriter(outputPath, true)); //creating the new buffered writer
                //iterating over each of the items that have been listed for saving
                for(OutputItem outItem : outputBuffer) {
                    buffWriter.write("%-40s%s%n", outItem.getHeading(), output.getValue());
                    buffWriter.newLine();
                }
                //closing the current writer
                buffWriter.flush();
                buffWriter.close();
            }
            catch (IOException error) { //catching a thrown error
                error.printStackTrace();
            }
        }
        else {

            System.out.println("Error: no items to output");
            return;
        }
    }

    /*
    method to add another item to be written to the output file
    string and value gets formatted when printed */
    public void stageItemForOutput(String heading, double value) {
        outputBuffer.add(new OutputItem(heading, value));
    }

    /*
    method to reset the default file path to a
    brand new String / change the entire directory*/
    public static void setFolderPath(String newFolderPath) {
        this.folderPath = newFolderPath;
    }

    /*
    method to set the name of the file that will be appended to the folder path
    file that the results will be written to */
    public static void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /*
    if the getters return the default values it means that
    the recorder has not been initiated yet */

    /*
    method to return the default file directory including
    any currently added file extensions */
    public static String getFolderPath() {
        return filePath;
    }

    /*
    method to return the string value
    of the current destination file being written to */
    public static String getFileName() {
        return fileName;
    }

}
