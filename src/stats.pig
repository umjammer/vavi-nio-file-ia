REGISTER s3://historycrawl/jars/archive-metadata-extractor-20110430.jar;
REGISTER s3://historycrawl/jars/jsplitter.jar;
REGISTER s3://historycrawl/jars/commons-validator-1.4.0.jar;

titles = LOAD 's3://historycrawl/inputs/' USING org.archive.hadoop.ArchiveJSONViewLoader('Envelope.Payload-Metadata.HTTP-Response-Metadata.HTML-Metadata','Envelope.ARC-Header-Metadata.Target-URI') AS (links:chararray,target:chararray);
nonnulls = filter titles by links is not null;
paths = foreach nonnulls generate org.sci.historycrawl.parser($0,target);
describe paths;
--i5 = limit paths 100;
i6 = foreach paths generate bagwati.url;          
i7 = foreach i6 generate flatten($0) as words;
i8 = group i7 by words;                     
i9 = foreach i8 generate group,COUNT(i7);
i10 = foreach i9 generate org.sci.historycrawl.splitter($0,$1);
store i10  INTO 's3://historycrawl/output3' using PigStorage();
