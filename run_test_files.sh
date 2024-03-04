#!/bin/bash

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

tests_dir="$script_dir/src/test/resources/stella-tests"
app="$script_dir/build/install/TypesProject/bin/TypesProject"

find "$tests_dir" -type f -name '*.st' | while read -r file; do
    echo "File: $file"
    echo "Source:"
    echo "\`\`\`"
    cat "$file"
    printf "\n\`\`\`\n"
    echo "Type check:"
    $app < "$file"
    echo "_______________________________________________________________________________________"
    printf "\n"
done
