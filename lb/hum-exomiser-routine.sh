#!/usr/bin/bash

MODE=$1 #two modes: "single" or "trio"
YML=$2
PEDF=$3

DATADIR=data
REF=/ramdisk/HUM-NGSDiag/GATK/human_g1k_v37.fasta
DBSNP=/ramdisk/HUM-NGSDiag/GATK/dbsnp_138.b37.vcf.gz

THREADS=32

if [ $MODE == "single" ]; then
  echo "Single Exomise with YAML File '$YAML' - Runtime:" >&2
  time java -Xms8g -Xmx64g -jar exomiser-cli-12.1.0.jar \
    --analysis $YML \
    >log/${YML##*/}.log 2>&1
fi

if [ $MODE == "trio" ]; then
  #Create MultiVCF
  FAMILY=($(cut -d "	" -f2 $PEDF))
  FAMILYS=$(echo "${FAMILY[*]}" | sed 's/ /+/g')
  GTVCFFILE=results/${FAMILYS}.ra.rc.gt.vcf
  VCFCMDS=$(for s in "${FAMILY[@]}"; do echo -n "--variant ${DATADIR}/$s.ra.rc.g.vcf.gz "; done )
  echo -n "MultiVCF for '$FAMILYS' - " >&2
  if [ ! -f "$GTVCFFILE.gz" ]; then
    echo "Creating... Runtime:" >&2
    conda activate HUM-NGSDiag
    #TODO: Might use bcftools merge here as it is much faster
    time {
      gatk3 -Xms8g -Xmx64g --analysis_type GenotypeGVCFs \
        --reference_sequence ${REF} \
        --dbsnp ${DBSNP} \
        --max_alternate_alleles 6 \
        --num_threads ${THREADS} \
        $VCFCMDS \
        --out ${GTVCFFILE}
      bgzip ${GTVCFFILE}
      tabix ${GTVCFFILE}.gz
    }
    conda deactivate
  else
    echo "Exists as '$GTVCFFILE'" >&2
  fi

  #Checking pedigree with peddy https://github.com/brentp/peddy
  echo -n "Peddyanalysis for for '$FAMILYS' - " >&2
  if [ ! -f "results/${FAMILYS}_peddy.html" ]; then
    echo "Processing... Runtime:" >&2
    conda activate HUM-peddy
    time {
      peddy --procs ${THREADS} --plot --loglevel INFO \
        --prefix results/${FAMILYS}_peddy \
        ${GTVCFFILE}.gz ${PEDF}
    }
    conda deactivate
  else
    echo "Exists as 'results/${FAMILYS}_peddy.html'" >&2
  fi

  #exomise ACMG
  echo "Trio Exomise with YAML File '$YML' - Runtime:" >&2
  time java -Xms8g -Xmx64g -jar exomiser-cli-12.1.0.jar \
    --analysis ${YML}
fi
