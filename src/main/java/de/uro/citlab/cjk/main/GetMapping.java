/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uro.citlab.cjk.main;

import de.uro.citlab.cjk.Decomposer;
import de.uro.citlab.cjk.util.DecomposerUtil;
import de.uro.citlab.cjk.util.FileUtil;
import de.uro.citlab.cjk.util.Gnuplot;
import eu.transkribus.errorrate.util.ObjectCounter;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gundram
 */
public class GetMapping {

    private static Logger LOG = LoggerFactory.getLogger(GetMapping.class);

    public static void main(String[] args) {
        for (Decomposer.Coding coding : Decomposer.Coding.values()) {
            File root = new File(".");
            File folder = new File(root, coding.toString());
            folder.mkdir();
            /////////////////////////////
            /// create raw decomposer ///
            /////////////////////////////
            Decomposer dec = new Decomposer(coding);

            //dump some stuff
            DecomposerUtil.saveCharSet(dec, new File(folder, "leaves.txt"), true);
            DecomposerUtil.saveMap(dec, new File(folder, "map.txt"), true, true);

            /////////////////////////
            /// minimize composer ///
            /////////////////////////
            List<File> listFiles = FileUtil.listFiles(new File(root, "docs"), "txt", true);
            ObjectCounter<Character> oc = new ObjectCounter<>();
            for (File listFile : listFiles) {
                List<String> readLines = FileUtil.readLines(listFile);
                for (String readLine : readLines) {
                    for (char sign : readLine.toCharArray()) {
                        oc.add(sign);
                        ///////////////////
                        /// count signs ///
                        ///////////////////
                        dec.count(String.valueOf(sign));
                    }
                }
            }

            System.out.println("number distinct signs in text: " + oc.getMap().size());
            ////////////////////////////////
            /// prune decomposition tree ///
            ////////////////////////////////
            Map<String, List<Double>> reduceDecomposer = DecomposerUtil.reduceDecomposer(dec, 0.005, coding.toString().endsWith("IDC") ? 7 : 11);
            //dump some stuff
            DecomposerUtil.saveCharSet(dec, new File(folder, "leaves_after.txt"), true);
            DecomposerUtil.saveMap(dec, new File(folder, "map_after.txt"), true, true);

//            boolean consistentWithoutFormatSigns = DecomposerUtil.isConsistentWithoutFormatSigns(dec);
//            System.out.println("is consistent with lower format: " + consistentWithoutFormatSigns);
            //show some stuff
            try {
                List<Double> xs = reduceDecomposer.get("size");
                reduceDecomposer.remove("size");
                List<double[]> ys = new LinkedList<>();
                String[] names = new String[reduceDecomposer.size()];
                int idx = 0;
                for (String name : reduceDecomposer.keySet()) {
                    names[idx++] = name;
                    ys.add(DecomposerUtil.toArray(reduceDecomposer.get(name)));
                }
                Gnuplot.withGrid = true;
                Gnuplot.plot(DecomposerUtil.toArray(xs), ys, "CharSet size compared to average decomposition length", names, null);

            } catch (Throwable ex) {
                LOG.warn("GNUplot not installed correctly (or windows is used) - skip display", ex);
                System.out.println("GNUplot not installed correctly (or windows is used) - skip display");
            }
//        System.out.println((int) '土');
//        System.out.println((int) '士');
        }
    }

}
