# CJK-decomposition

The decomposer can be used to split Chinese, Japanese or Korean signs into smaller components.
This mapping from original character to smaller components we call "decomposition".
The goal is to have a decomposition of each sign into smaller components, whereas we only allow decompositions which are part of the actual unicode and are storeable by UTF-8 coding

As decomposition basis we use the 'ids.txt' which is a copy of https://github.com/cjkvi/cjkvi-ids/blob/master/ids.txt.

## Algorithm to decompose Characters
* the for each sign a direct decomposition is loaded
* we apply the decomposition recursively to each sign
* at the end, each sign has a final decomposition, which is a kind of decomposition-tree
* We take the leaves of the tree as raw decomposition
* we truncate the tree as long as the leaves are valid UTF-8 codes or until the sign is empty (this only happens, if the sign itself and its decomposition variants have no UTF-8 code)
* the leaves (which are UTF-8 codes) are the basis components (we call them atoms) of the mapping
* this set of leaves is called 'CharSet'.

## Algorithm to minimize decomposition length (reduce decomposition)
In CJK languages there are signs that occure very often. For these signs it makes sense, to define them as leaves (if they have a valid UTF-8 code) and therefore reduce the decomposition length of signs:
This method can only work for a given text resource, for which we can optimize the CharSet. The goal is to make some signs to leaves so that the average decomposition length of a sign is reduced.

* for a given text resource count the number of occurance of all signs and recursively all decomposition parts
* do: (reduce average length)
    * for each sign we calculate 'count*(length-1)' which is a score for how much it would reduce the text decomposition length.
    * we define the sign with maximal score as new leaf.
    * repeat until the reduction gained by a new leaf is too small.
* do: (reduce length of all decompositions to a maximal value)
    * find root-sign which occures in the text resource with the largest decomposition length
    * find all sign in decomposition tree that makes length of the root-sign <= maximal value
    * we define the sign with maximal 'count*(length-1)' as new leaf
    * repeat until each root-sign has a decomposition <= maximal value
* do: (make decomposition -> root-sign distinct)
    * find sign-pairs which have the same decomposition
    * make both signs to leaves
    * repeat until no more sign-pairs are found

## Decomposition variants
there are 2 variant how a decomposition can be done:
1. The CharSet also contians character, which have no UTF-8 code. This makes the decomposition a bit easier, but the CharSet is no valid UTF-8 code.
2. For the decomposition we ignore all Ideographic Description Characters. This reduces the average length of the decomposition by a factor of ~30%, but makes it (almost) impossible to remap decomposition to a sign.


for other (eventually better) mappings see
- https://github.com/cjkvi/cjkvi-ids
- https://github.com/chise/isd
- https://github.com/kawabata/ids




