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
import de.uro.citlab.cjk.util.ObjectCounter;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gundram
 */
public class GetMapping {

    private static Logger LOG = LoggerFactory.getLogger(GetMapping.class);
    private final Options options = new Options();

    public GetMapping() {
        options.addOption("h", "help", false, "show this help");
        options.addOption("r", "raw", false, "do not prune mapping");
        options.addOption("u", "utf8", false, "only use valid utf-8 characters as leaf");
        options.addOption("i", "idc", false, "delete ideographic description characters for decomposition");
        options.addOption("o", "out-map", true, "path to save output file");
        options.addOption("l", "out-leaves", true, "path to save character set to file");
        options.addOption("p", "plot", false, "plot reduction process");
        options.addOption("m", "maxlength", true, "maximal length of decomposition (default: 11 (with -d: 7)");
        options.addOption("g", "gain", true, "minimal gain for adding additional leaves (default: 0.005)");
    }

    public static void main(String[] args) throws IOException {
        GetMapping mapping = new GetMapping();
        mapping.run(args);
    }

    public void run(String[] args) {
        CommandLine cmd = null;
        try {
            cmd = new DefaultParser().parse(options, args);

            //Help?
            if (cmd.hasOption("h")) {
                help();
            }
            Decomposer.Coding coding = null;
            if (cmd.hasOption('u')) {
                coding = cmd.hasOption('i') ? Decomposer.Coding.UTF8_IDC : Decomposer.Coding.UTF8;
            } else {
                coding = cmd.hasOption('i') ? Decomposer.Coding.IDC : Decomposer.Coding.ANY;
            }
            int maxlen = cmd.hasOption('m') ? Integer.parseInt(cmd.getOptionValue('m')) : cmd.hasOption('i') ? 7 : 11;
            double gain = cmd.hasOption('g') ? Double.parseDouble(cmd.getOptionValue('g')) : 0.005;

            /////////////////////////////
            /// create raw decomposer ///
            /////////////////////////////
            Decomposer dec = new Decomposer(coding);
            if (!cmd.hasOption('r')) {
                if (cmd.getArgList().isEmpty()) {
                    help("if -r is not set, a list files have to be given, but arg-list is empty.");
                }
                /////////////////////////
                /// minimize composer ///
                /////////////////////////
                ObjectCounter<Character> oc = new ObjectCounter<>();
                if (cmd.getArgList().size() == 1 && cmd.getArgList().get(0).equals("?")) {
                    try {
                        String str = new String(IOUtils.toByteArray(this.getClass().getClassLoader().getResourceAsStream("dummy.txt")));
                        for (char c : str.toCharArray()) {
                            oc.add(c);
                            ///////////////////
                            /// count chars ///
                            ///////////////////
                            dec.count(String.valueOf(c));
                        }
                    } catch (IOException e) {
                        help("resource 'dummy.txt' cannot be found in jar", e);
                    }
                } else {
                    List<File> files = new LinkedList<>();
                    String prefix = "";
                    for (int i = 0; i < cmd.getArgList().size(); i++) {
                        String get = cmd.getArgList().get(i);
                        prefix += " " + get;
                        File file = new File(prefix.trim());
                        if (file.exists()) {
                            files.add(file);
                            prefix = "";
                        }
                    }
                    if (files.isEmpty()) {
                        help("cannot load files " + cmd.getArgList().toString());
                    }
//                    List<File> listFiles = FileUtil.listFiles(new File(cmd.getOptionValue('i')), "txt", true);
                    LOG.info("found " + files.size() + " files to use as character resources");
                    for (File listFile : files) {
                        List<String> readLines = null;
                        try {
                            readLines = FileUtil.readLines(listFile);
                        } catch (RuntimeException e) {
                            help("file '" + listFile.getAbsolutePath() + "' cannot be found", e);
                        }
                        for (String readLine : readLines) {
                            for (char c : readLine.toCharArray()) {
                                oc.add(c);
                                ///////////////////
                                /// count chars ///
                                ///////////////////
                                dec.count(String.valueOf(c));
                            }
                        }
                    }
                }

                if (oc.getMap().isEmpty()) {
                    help("no text file found or loaded");
                }
                System.out.println("number distinct characters in text: " + oc.getMap().size());
                ////////////////////////////////
                /// prune decomposition tree ///
                ////////////////////////////////
                Map<String, List<Double>> reduceDecomposer = DecomposerUtil.reduceDecomposer(dec, gain, maxlen);

                //show some stuff
                if (cmd.hasOption('p')) {
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
                        Gnuplot.plot(DecomposerUtil.toArray(xs), ys, "CharSet size compared to average decomposition length", names, "example_decomposition.png", null);

                    } catch (Throwable ex) {
                        LOG.warn("GNUplot not installed correctly (or windows is used) - please unset '-p'", ex);
                        help("GNUplot not installed correctly (or windows is used) - please unset '-p'", ex);
                    }
                }
            }

            //dump some stuff
            if (cmd.hasOption('l')) {
                DecomposerUtil.saveCharSet(dec, new File(cmd.getOptionValue('l')), true);
            }
            if (cmd.hasOption('o')) {
                DecomposerUtil.saveMap(dec, new File(cmd.getOptionValue('o')), true, true);
            }
        } catch (ParseException ex) {
            help("Failed to parse comand line properties", ex);
        }
    }

    private void help() {
        help(null, null);
    }

    private void help(String suffix) {
        help(suffix, null);
    }

    private void help(String suffix, Throwable e) {
        // This prints out some help
        if (suffix != null && !suffix.isEmpty()) {
            suffix = "ERROR:\n" + suffix;
            if (e != null) {
                suffix += "\n" + e.getMessage();
            }
        }
        HelpFormatter formater = new HelpFormatter();
        formater.printHelp(
                "java -jar CJK-decomposition.jar ( ? | <textfile_utf8-1> <textfile_utf8-2> ... )",
                "This method can be used to create a mapping to decompose chinese, japanese (Kanji) "
                + "or Korean (Janja) characters into simpler parts. Either set args '?' of a list of textfile, "
                + "which are utf-8 coded.",
                options,
                suffix,
                true
        );
        System.exit(0);
    }

}
