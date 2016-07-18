#
# Generate triples in Karma's offline mode. Pass in:
# CSV file
# Model file
# Output destination
#

echo ""
echo "Generating batch of triples"
echo "Source $1"
echo "Model $2"
echo

OUTPUT=${3:-triples.nt}

echo "Output $OUTPUT"

mvn exec:java -Dexec.mainClass="edu.isi.karma.rdf.OfflineRdfGenerator" \
-Dexec.args="--sourcetype CSV --filepath $1 --modelfilepath $2 --sourcename $OUTPUT --outputfile $OUTPUT" \
-Dexec.classpathScope=compile
