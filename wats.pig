SET pig.splitCombination 'false';
 
-- Load Internet Archive Pig utility jar:
REGISTER /home/sumanth/Desktop/pjar/archive-metadata-extractor-20110430.jar;
 
-- alias short-hand for IA 'resolve()' UDF:
DEFINE resolve org.archive.hadoop.func.URLResolverFunc();
 
-- load data from SRC_SPEC:
Orig = LOAD '$SRC_SPEC' USING
org.archive.hadoop.ArchiveJSONViewLoader('Envelope.WARC-Header-Metadata.WARC-Target-URI',
'Envelope.Payload-Metadata.HTTP-Response-Metadata.HTML-Metadata.Head.Base','Envelope.Payload-Metadata.HTTP-Response-Metadata.HTML-Metadata.@Links.url')
AS (page_url,html_base,relative);
 
-- discard lines without links
LinksOnly = FILTER Orig BY relative != '';
 
-- fabricate new 1st column, which is the resolved to-URL, followed by the from-URL:
ResolvedLinks = FOREACH LinksOnly GENERATE FLATTEN(resolve(page_url,html_base,relative)) AS (resolved), page_url;
 
-- this will include all the fields, for debug:
--ResolvedLinks = FOREACH LinksOnly GENERATE FLATTEN(resolve(page_url,html_base,relative)) AS (resolved), page_url, html_base, relative;
 
SortedLinks = ORDER ResolvedLinks BY resolved, page_url;
 
STORE SortedLinks INTO '$TGT_DIR' USING PigStorage();
