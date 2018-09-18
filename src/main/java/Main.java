import net.semanticmetadata.lire.aggregators.BOVW;
import net.semanticmetadata.lire.builders.DocumentBuilder;
import net.semanticmetadata.lire.builders.GlobalDocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.features.global.CEDD;
import net.semanticmetadata.lire.imageanalysis.features.local.simple.SimpleExtractor;
import net.semanticmetadata.lire.indexers.parallel.ParallelIndexer;
import net.semanticmetadata.lire.searchers.BitSamplingImageSearcher;
import net.semanticmetadata.lire.searchers.GenericFastImageSearcher;
import net.semanticmetadata.lire.searchers.ImageSearchHits;
import net.semanticmetadata.lire.searchers.ImageSearcher;
import net.semanticmetadata.lire.utils.FileUtils;
import net.semanticmetadata.lire.utils.LuceneUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

public class Main {
    public static int numOfClusters = 512;

    public static void main(String[] args) throws IOException {
//        index(args);
        search(args);
    }

    static void index(String[] args) {
        // Checking if arg[0] is there and if it is a directory.
        boolean passed = false;
        if (args.length > 0) {
            File f = new File(args[0]);
            System.out.println("Indexing images in " + args[0]);
            if (f.exists() && f.isDirectory()) passed = true;
        }
        if (!passed) {
            System.out.println("No directory given as first argument.");
            System.out.println("Run \"Indexer <directory>\" to index files of a directory.");
            System.exit(1);
        }
        // Getting all images from a directory and its sub directories.
        ArrayList<String> images = null;
        try {
            images = FileUtils.readFileLines(new File(args[0]), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ParallelIndexer indexer = new ParallelIndexer(8, "index", args[0],  GlobalDocumentBuilder.HashingMode.BitSampling);

        indexer.addExtractor(CEDD.class, SimpleExtractor.KeypointDetector.CVSURF);
        indexer.run();

        System.out.println("Finished indexing.");

//        // Creating a CEDD document builder and indexing all files.
//        GlobalDocumentBuilder globalDocumentBuilder = new GlobalDocumentBuilder(CEDD.class);
//        // Creating an Lucene IndexWriter
//        IndexWriter iw = null;
//        try {
//            iw = LuceneUtils.createIndexWriter("index", true, LuceneUtils.AnalyzerType.WhitespaceAnalyzer);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        // Iterating through images building the low level features
//        for (Iterator<String> it = images.iterator(); it.hasNext(); ) {
//            String imageFilePath = it.next();
//            System.out.println("Indexing " + imageFilePath);
//            try {
//                BufferedImage img = ImageIO.read(new FileInputStream(imageFilePath));
//                Document document = globalDocumentBuilder.createDocument(img, imageFilePath);
//                iw.addDocument(document);
//            } catch (Exception e) {
//                System.err.println("Error reading image or indexing it.");
//                e.printStackTrace();
//            }
//        }
//        // closing the IndexWriter
//        try {
//            LuceneUtils.closeWriter(iw);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println("Finished indexing.");
    }

    static void search(String[] args) throws IOException {
        // Checking if arg[0] is there and if it is an image.
        BufferedImage img = null;
        boolean passed = false;
        String filepath = "/home/michu/Desktop/vay_do.jpg";
        if (args.length > 1) {
            File f = new File(filepath);
            if (f.exists()) {
                try {
                    img = ImageIO.read(f);
                    passed = true;
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        if (!passed) {
            System.out.println("No image given as first argument.");
            System.out.println("Run \"Searcher <query image>\" to search for <query image>.");
            System.exit(1);
        }

        IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get("index")));
        ImageSearcher searcher = new BitSamplingImageSearcher(30, new CEDD());

        searcher = new GenericFastImageSearcher(10, CEDD.class, SimpleExtractor.KeypointDetector.CVSURF, new BOVW(), numOfClusters, true, ir, "index" + ".config");

        // searching with a image file ...
        long start = System.currentTimeMillis();

        ImageSearchHits hits = searcher.search(img, ir);
        // searching with a Lucene document instance ...

        long end = System.currentTimeMillis();
        System.out.println(end - start);

        for (int i = 0; i < hits.length(); i++) {
            String fileName = ir.document(hits.documentID(i)).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
            System.out.println(hits.score(i) + ": \t" + fileName);
        }
    }
}
