#!/bin/bash

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

tests_dir="$script_dir/src/test/resources/stella-tests"
app_jar="$script_dir/build/libs/TypesProject-1.0-SNAPSHOT.jar"

find "$tests_dir" -type f -name '*.st' | while read -r file; do
    echo "File: $file"
    echo "Source:"
    echo "\`\`\`"
    cat "$file"
    printf "\n\`\`\`\n"
    echo "Type check:"
    java -jar "$app_jar" < "$file"
    echo "_______________________________________________________________________________________"
    printf "\n"
done
